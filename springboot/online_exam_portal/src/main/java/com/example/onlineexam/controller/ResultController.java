package com.example.onlineexam.controller;

import com.example.onlineexam.model.Result;
import com.example.onlineexam.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    @Autowired
    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/student/{studentId}/exam/{examId}")
    public ResponseEntity<Result> getResultForStudentAndExam(
            @PathVariable Long studentId,
            @PathVariable Long examId) {
        Result result = resultService.getResultForStudentAndExam(studentId, examId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Result>> getResultsForStudent(@PathVariable Long studentId) {
        List<Result> results = resultService.getResultsForStudent(studentId);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<List<Result>> getResultsForExam(@PathVariable Long examId) {
        List<Result> results = resultService.getResultsForExam(examId);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}
