package com.example.onlineexam.controller;

import com.example.onlineexam.dto.ExamSubmissionRequest;
import com.example.onlineexam.model.Exam;
import com.example.onlineexam.model.Result;
import com.example.onlineexam.service.ExamService;
import com.example.onlineexam.service.ResultService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final ResultService resultService;

    @Autowired
    public ExamController(ExamService examService, ResultService resultService) {
        this.examService = examService;
        this.resultService = resultService;
    }

    @PostMapping
    public ResponseEntity<Exam> createExam(@Valid @RequestBody Exam exam) {
        Exam created = examService.createExam(exam);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/{examId}/start")
    public ResponseEntity<Result> startExam(@PathVariable Long examId, @RequestParam Long studentId) {
        Result result = resultService.startExam(examId, studentId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/{examId}/submit")
    public ResponseEntity<Result> submitExam(@PathVariable Long examId, @Valid @RequestBody ExamSubmissionRequest request) {
        Result result = resultService.submitExam(examId, request);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/{examId}/publish")
    public ResponseEntity<String> publishResults(@PathVariable Long examId) {
        examService.publishResults(examId);
        return new ResponseEntity<>("Results published successfully.", HttpStatus.OK);
    }
}
