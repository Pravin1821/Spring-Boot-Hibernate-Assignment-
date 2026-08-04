package com.example.onlineexam.service;

import com.example.onlineexam.model.Exam;
import com.example.onlineexam.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamService {

    private final ExamRepository examRepository;

    @Autowired
    public ExamService(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Transactional
    public Exam createExam(Exam exam) {
        // Enforce bidirectional relationship for cascade save
        if (exam.getQuestions() != null) {
            exam.getQuestions().forEach(q -> q.setExam(exam));
        }
        return examRepository.save(exam);
    }

    public Exam getExamById(Long id) {
        return examRepository.findById(id).orElse(null);
    }

    @Transactional
    public Exam publishResults(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with id: " + examId));
        exam.setResultsPublished(true);
        return examRepository.save(exam);
    }
}
