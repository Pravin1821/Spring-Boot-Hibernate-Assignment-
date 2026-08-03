package com.example.jwtauth;

import com.example.jwtauth.dto.LoginRequest;
import com.example.jwtauth.dto.ProfileUpdateRequest;
import com.example.jwtauth.dto.RegisterRequest;
import com.example.jwtauth.model.User;
import com.example.jwtauth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class JwtAuthSystemApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    private String generateExpiredToken(String username) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(key)
                .compact();
    }

    @Test
    public void testSuccessfulRegisterLoginAndProfileFlow() throws Exception {
        // 1. Register User
        RegisterRequest registerReq = new RegisterRequest(
                "john_doe",
                "SecureP@ss123",
                "john@example.com",
                "John",
                "Doe"
        );

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andReturn();

        // 2. Login User
        LoginRequest loginReq = new LoginRequest("john_doe", "SecureP@ss123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        // 3. Get Profile (Secured)
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        // 4. Update Profile (Secured)
        ProfileUpdateRequest updateReq = new ProfileUpdateRequest(
                "john_updated@example.com",
                "Johnny",
                "DoeUpdated"
        );

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john_updated@example.com"))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("DoeUpdated"));
    }

    @Test
    public void testInvalidCredentials() throws Exception {
        // Save user first
        User user = new User("testuser", passwordEncoder.encode("Password@123"), "test@test.com", "Test", "User");
        userRepository.save(user);

        LoginRequest badLogin = new LoginRequest("testuser", "WrongPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid username or password")));
    }

    @Test
    public void testExpiredJwt() throws Exception {
        String expiredToken = generateExpiredToken("expired_user");

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("JWT token has expired")));
    }

    @Test
    public void testMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Missing or invalid Authorization header")));
    }

    @Test
    public void testDuplicateUsernames() throws Exception {
        RegisterRequest registerReq1 = new RegisterRequest(
                "dup_user",
                "SecureP@ss123",
                "dup1@example.com",
                "Dup",
                "One"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq1)))
                .andExpect(status().isOk());

        RegisterRequest registerReq2 = new RegisterRequest(
                "dup_user",
                "AnotherP@ss999",
                "dup2@example.com",
                "Dup",
                "Two"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Username already exists")));
    }

    @Test
    public void testPasswordValidation() throws Exception {
        // Too short, no digit, no uppercase, no special character
        RegisterRequest weakRequest = new RegisterRequest(
                "weak_user",
                "pass",
                "weak@example.com",
                "Weak",
                "User"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errors.password").exists());

        // Standard pattern check: minimum 8 characters, 1 uppercase, 1 lowercase, 1 number, 1 special char.
        RegisterRequest noSpecialCharRequest = new RegisterRequest(
                "weak_user2",
                "Pass123456",
                "weak2@example.com",
                "Weak",
                "User"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noSpecialCharRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    public void testAccessingSecuredApisWithoutAuthentication() throws Exception {
        // Without Authorization Header
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());

        // With garbage token
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer invalidtokengarbagehere"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid JWT token")));
    }
}
