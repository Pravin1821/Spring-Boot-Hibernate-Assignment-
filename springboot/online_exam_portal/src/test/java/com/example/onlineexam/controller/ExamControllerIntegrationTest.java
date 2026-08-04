package com.example.onlineexam.controller;

import com.example.onlineexam.dto.AnswerSubmission;
import com.example.onlineexam.dto.ExamSubmissionRequest;
import com.example.onlineexam.model.Exam;
import com.example.onlineexam.model.Question;
import com.example.onlineexam.model.Result;
import com.example.onlineexam.model.Student;
import com.example.onlineexam.repository.ExamRepository;
import com.example.onlineexam.repository.ResultRepository;
import com.example.onlineexam.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ExamControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Student registeredStudent;
    private Exam activeExam;

    @BeforeEach
    public void setup() {
        resultRepository.deleteAll();
        examRepository.deleteAll();
        studentRepository.deleteAll();

        // 1. Create and save a registered student
        registeredStudent = new Student("Alice Test", "alice@example.com");
        registeredStudent = studentRepository.save(registeredStudent);

        // 2. Create and save an exam with questions
        activeExam = new Exam("Web Basics", "Quiz on HTML and CSS", 10);
        Question q1 = new Question("What does HTML stand for?", 
                "Hyper Text Markup Language", "Hyperlinks and Text Markup", 
                "Home Tool Markup Language", "Hyperlinks Text Markup", "A", 5);
        Question q2 = new Question("What does CSS stand for?", 
                "Computer Style Sheets", "Creative Style Sheets", 
                "Cascading Style Sheets", "Colorful Style Sheets", "C", 5);

        activeExam.addQuestion(q1);
        activeExam.addQuestion(q2);

        activeExam = examRepository.save(activeExam);
    }

    @Test
    public void testRegisterStudent_Success() throws Exception {
        Student newStudent = new Student("Bob Test", "bob@example.com");

        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newStudent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Bob Test")))
                .andExpect(jsonPath("$.email", is("bob@example.com")));
    }

    @Test
    public void testRegisterStudent_DuplicateEmail() throws Exception {
        Student duplicateStudent = new Student("Alice Replica", "alice@example.com");

        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateStudent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    public void testCreateExam_Success() throws Exception {
        Exam newExam = new Exam("Java Basics", "Java standard concepts", 15);
        Question q = new Question("Size of double in Java?", "4 bytes", "8 bytes", "12 bytes", "16 bytes", "B", 10);
        newExam.addQuestion(q);

        mockMvc.perform(post("/api/exams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newExam)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Java Basics")))
                .andExpect(jsonPath("$.durationInMinutes", is(15)))
                .andExpect(jsonPath("$.questions", hasSize(1)));
    }

    @Test
    public void testStartAndSubmitExam_Success_AutoCalculatesMarks() throws Exception {
        // Start exam session
        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/start")
                .param("studentId", registeredStudent.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.submitted", is(false)));

        // Prepare submission with answers: Q1 = A (Correct, 5 marks), Q2 = D (Incorrect, correct is C, 0 marks)
        List<Question> questions = activeExam.getQuestions();
        Long q1Id = questions.get(0).getId();
        Long q2Id = questions.get(1).getId();

        AnswerSubmission ans1 = new AnswerSubmission(q1Id, "A");
        AnswerSubmission ans2 = new AnswerSubmission(q2Id, "D");

        ExamSubmissionRequest request = new ExamSubmissionRequest(registeredStudent.getId(), Arrays.asList(ans1, ans2));

        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submitted", is(true)))
                .andExpect(jsonPath("$.marksObtained", is(5))) // Q1 correct (5), Q2 incorrect (0)
                .andExpect(jsonPath("$.totalMarks", is(10)))
                .andExpect(jsonPath("$.timeExceeded", is(false)));
    }

    @Test
    public void testSubmitExam_StudentNotRegistered() throws Exception {
        // Try submitting answers for an unregistered student ID (9999)
        AnswerSubmission ans = new AnswerSubmission(activeExam.getQuestions().get(0).getId(), "A");
        ExamSubmissionRequest request = new ExamSubmissionRequest(9999L, Arrays.asList(ans));

        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is not registered")));
    }

    @Test
    public void testSubmitExam_DuplicateSubmission() throws Exception {
        // Start and submit exam first
        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/start")
                .param("studentId", registeredStudent.getId().toString()))
                .andExpect(status().isOk());

        Long q1Id = activeExam.getQuestions().get(0).getId();
        AnswerSubmission ans = new AnswerSubmission(q1Id, "A");
        ExamSubmissionRequest request = new ExamSubmissionRequest(registeredStudent.getId(), Arrays.asList(ans));

        // Submit first time
        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Submit second time
        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already submitted")));
    }

    @Test
    public void testSubmitExam_InvalidQuestionIds() throws Exception {
        // Start exam
        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/start")
                .param("studentId", registeredStudent.getId().toString()))
                .andExpect(status().isOk());

        // Use a random invalid question ID (9999)
        AnswerSubmission ans = new AnswerSubmission(9999L, "A");
        ExamSubmissionRequest request = new ExamSubmissionRequest(registeredStudent.getId(), Arrays.asList(ans));

        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to this exam")));
    }

    @Test
    public void testSubmitExam_TimeLimitExceeded() throws Exception {
        // Start exam session
        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/start")
                .param("studentId", registeredStudent.getId().toString()))
                .andExpect(status().isOk());

        // Retrieve the generated Result in DB, and manually backdate it to simulate time limit exceeded
        Result dbResult = resultRepository.findByStudentIdAndExamIdAndSubmitted(registeredStudent.getId(), activeExam.getId(), false)
                .orElseThrow(() -> new RuntimeException("Test setup failure - Result attempt not found"));

        // Backdate to 2 hours ago (Exam duration is 10 minutes)
        dbResult.setStartTime(LocalDateTime.now().minusHours(2));
        resultRepository.save(dbResult);

        // Submit answers
        Long q1Id = activeExam.getQuestions().get(0).getId();
        AnswerSubmission ans = new AnswerSubmission(q1Id, "A");
        ExamSubmissionRequest request = new ExamSubmissionRequest(registeredStudent.getId(), Arrays.asList(ans));

        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Time limit exceeded")));
    }

    @Test
    public void testSubmitExam_EmptyExam() throws Exception {
        // Create an empty exam (0 questions)
        Exam emptyExam = new Exam("Empty Quiz", "No questions here", 15);
        emptyExam = examRepository.save(emptyExam);

        // Try starting the empty exam
        mockMvc.perform(post("/api/exams/" + emptyExam.getId() + "/start")
                .param("studentId", registeredStudent.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot start an empty exam")));
    }

    @Test
    public void testPublishAndRetrieveResults() throws Exception {
        // Start and submit answers
        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/start")
                .param("studentId", registeredStudent.getId().toString()))
                .andExpect(status().isOk());

        Long q1Id = activeExam.getQuestions().get(0).getId();
        AnswerSubmission ans = new AnswerSubmission(q1Id, "A");
        ExamSubmissionRequest request = new ExamSubmissionRequest(registeredStudent.getId(), Arrays.asList(ans));

        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Attempting to retrieve results before publishing should throw BAD_REQUEST
        mockMvc.perform(get("/api/results/student/" + registeredStudent.getId() + "/exam/" + activeExam.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("have not been published yet")));

        // Publish results
        mockMvc.perform(post("/api/exams/" + activeExam.getId() + "/publish"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Results published successfully")));

        // Fetching results should now succeed
        mockMvc.perform(get("/api/results/student/" + registeredStudent.getId() + "/exam/" + activeExam.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marksObtained", is(5)))
                .andExpect(jsonPath("$.totalMarks", is(10)));
    }
}
