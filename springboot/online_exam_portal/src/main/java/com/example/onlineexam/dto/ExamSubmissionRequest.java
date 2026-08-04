package com.example.onlineexam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ExamSubmissionRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Answers list is required")
    @Valid
    private List<AnswerSubmission> answers;

    public ExamSubmissionRequest() {
    }

    public ExamSubmissionRequest(Long studentId, List<AnswerSubmission> answers) {
        this.studentId = studentId;
        this.answers = answers;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public List<AnswerSubmission> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerSubmission> answers) {
        this.answers = answers;
    }
}
