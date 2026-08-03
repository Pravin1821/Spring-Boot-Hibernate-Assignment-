package com.example.productinventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.productinventory.model.Product;
import com.example.productinventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        productRepository.deleteAll();
    }

    @Test
    public void testAddProduct_Success() throws Exception {
        Product product = new Product("Laptop", "Electronics", new BigDecimal("999.99"), 10, "Dell Store");
        
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.category", is("Electronics")))
                .andExpect(jsonPath("$.price", is(999.99)))
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.supplierName", is("Dell Store")));
    }

    @Test
    public void testAddProduct_DuplicateName() throws Exception {
        Product p1 = new Product("Laptop", "Electronics", new BigDecimal("999.99"), 10, "Dell Store");
        productRepository.save(p1);

        Product p2 = new Product("Laptop", "Tech", new BigDecimal("1200.00"), 5, "HP Store");

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    public void testAddProduct_NegativePrice() throws Exception {
        Product product = new Product("Phone", "Electronics", new BigDecimal("-9.99"), 10, "Store");

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.details", hasItem(containsString("Price cannot be negative"))));
    }

    @Test
    public void testAddProduct_NegativeQuantity() throws Exception {
        Product product = new Product("Phone", "Electronics", new BigDecimal("500.00"), -5, "Store");

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.details", hasItem(containsString("Quantity cannot be negative"))));
    }

    @Test
    public void testUpdateProduct_Success() throws Exception {
        Product existing = productRepository.save(new Product("Laptop", "Electronics", new BigDecimal("999.99"), 10, "Dell Store"));
        Product updated = new Product("Laptop Pro", "Electronics", new BigDecimal("1299.99"), 8, "Dell Store");

        mockMvc.perform(put("/api/products/" + existing.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Laptop Pro")))
                .andExpect(jsonPath("$.price", is(1299.99)))
                .andExpect(jsonPath("$.quantity", is(8)));
    }

    @Test
    public void testUpdateProduct_NotFound() throws Exception {
        Product updated = new Product("Laptop Pro", "Electronics", new BigDecimal("1299.99"), 8, "Dell Store");

        mockMvc.perform(put("/api/products/9999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    public void testDeleteProduct_Success() throws Exception {
        Product existing = productRepository.save(new Product("Laptop", "Electronics", new BigDecimal("999.99"), 10, "Dell Store"));

        mockMvc.perform(delete("/api/products/" + existing.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + existing.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteProduct_NotFound() throws Exception {
        mockMvc.perform(delete("/api/products/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSearchAndFilters() throws Exception {
        productRepository.save(new Product("A_Laptop", "Electronics", new BigDecimal("1000.00"), 10, "Store A"));
        productRepository.save(new Product("B_Keyboard", "Electronics", new BigDecimal("50.00"), 3, "Store B"));
        productRepository.save(new Product("C_Apple", "Grocery", new BigDecimal("2.50"), 100, "Store C"));

        // Search by category
        mockMvc.perform(get("/api/products?category=Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));

        // Search by price range
        mockMvc.perform(get("/api/products?minPrice=10&maxPrice=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("B_Keyboard")));

        // Search by quantity threshold (retrieve products with quantity < specified threshold)
        mockMvc.perform(get("/api/products?threshold=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("B_Keyboard")));
        
        // Sorting by price asc
        mockMvc.perform(get("/api/products?sortBy=price&sortDir=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name", is("C_Apple")))
                .andExpect(jsonPath("$.content[1].name", is("B_Keyboard")))
                .andExpect(jsonPath("$.content[2].name", is("A_Laptop")));

        // Sorting by name desc
        mockMvc.perform(get("/api/products?sortBy=name&sortDir=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name", is("C_Apple")))
                .andExpect(jsonPath("$.content[1].name", is("B_Keyboard")))
                .andExpect(jsonPath("$.content[2].name", is("A_Laptop")));

        // Empty search results
        mockMvc.perform(get("/api/products?category=NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty()))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }
}
