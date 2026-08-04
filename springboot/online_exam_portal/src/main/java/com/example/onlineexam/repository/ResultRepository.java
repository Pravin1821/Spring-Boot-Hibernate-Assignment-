package com.example.onlineexam.repository;

import com.example.onlineexam.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    Optional<Result> findByStudentIdAndExamIdAndSubmitted(Long studentId, Long examId, boolean submitted);
    Optional<Result> findByStudentIdAndExamId(Long studentId, Long examId);
    List<Result> findByStudentId(Long studentId);
    List<Result> findByExamId(Long examId);
}
