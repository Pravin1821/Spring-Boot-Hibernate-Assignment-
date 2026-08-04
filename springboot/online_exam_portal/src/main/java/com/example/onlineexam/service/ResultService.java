package com.example.onlineexam.service;

import com.example.onlineexam.dto.AnswerSubmission;
import com.example.onlineexam.dto.ExamSubmissionRequest;
import com.example.onlineexam.exception.*;
import com.example.onlineexam.model.Exam;
import com.example.onlineexam.model.Question;
import com.example.onlineexam.model.Result;
import com.example.onlineexam.model.Student;
import com.example.onlineexam.repository.ExamRepository;
import com.example.onlineexam.repository.ResultRepository;
import com.example.onlineexam.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final ExamRepository examRepository;

    @Autowired
    public ResultService(ResultRepository resultRepository,
                         StudentRepository studentRepository,
                         ExamRepository examRepository) {
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.examRepository = examRepository;
    }

    @Transactional
    public Result startExam(Long examId, Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotRegisteredException("Student with id " + studentId + " is not registered."));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with id: " + examId));

        if (exam.getQuestions().isEmpty()) {
            throw new EmptyExamException("Cannot start an empty exam.");
        }

        // Check if student has already submitted this exam
        if (resultRepository.findByStudentIdAndExamId(studentId, examId)
                .map(Result::isSubmitted)
                .orElse(false)) {
            throw new DuplicateSubmissionException("Student has already submitted this exam.");
        }

        // Return or start attempt
        Result result = resultRepository.findByStudentIdAndExamIdAndSubmitted(studentId, examId, false)
                .orElse(new Result(student, exam, LocalDateTime.now()));
        result.setStartTime(LocalDateTime.now());
        return resultRepository.save(result);
    }

    @Transactional
    public Result submitExam(Long examId, ExamSubmissionRequest request) {
        Long studentId = request.getStudentId();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotRegisteredException("Student with id " + studentId + " is not registered."));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with id: " + examId));

        if (exam.getQuestions().isEmpty()) {
            throw new EmptyExamException("Cannot submit answers for an empty exam.");
        }

        // Look up result attempt
        Result result = resultRepository.findByStudentIdAndExamIdAndSubmitted(studentId, examId, false)
                .orElse(null);

        if (result == null) {
            // Check if already submitted
            boolean alreadySubmitted = resultRepository.findByStudentIdAndExamId(studentId, examId)
                    .map(Result::isSubmitted)
                    .orElse(false);
            if (alreadySubmitted) {
                throw new DuplicateSubmissionException("Student has already submitted this exam.");
            } else {
                throw new ExamNotStartedException("Student must start the exam before submitting answers.");
            }
        }

        // Check for invalid question IDs in submission
        Set<Long> examQuestionIds = exam.getQuestions().stream()
                .map(Question::getId)
                .collect(Collectors.toSet());

        for (AnswerSubmission answer : request.getAnswers()) {
            if (!examQuestionIds.contains(answer.getQuestionId())) {
                throw new InvalidQuestionException("Invalid question ID: " + answer.getQuestionId() + " does not belong to this exam.");
            }
        }

        // Check if time limit is exceeded
        LocalDateTime submitTime = LocalDateTime.now();
        long elapsedSeconds = Duration.between(result.getStartTime(), submitTime).toSeconds();
        long allowedSeconds = exam.getDurationInMinutes() * 60L;

        // 5 seconds grace period for network latency
        if (elapsedSeconds > allowedSeconds + 5) {
            result.setTimeExceeded(true);
            result.setSubmitted(true);
            result.setSubmitTime(submitTime);
            result.setMarksObtained(0);
            result.setTotalMarks(exam.getQuestions().stream().mapToInt(Question::getMarks).sum());
            resultRepository.save(result);
            throw new TimeLimitExceededException("Time limit exceeded for this exam. Submission is invalid.");
        }

        // Calculate marks
        int marksObtained = 0;
        Map<Long, Question> questionMap = exam.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        for (AnswerSubmission answer : request.getAnswers()) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question != null && answer.getSelectedOption() != null) {
                if (answer.getSelectedOption().trim().equalsIgnoreCase(question.getCorrectOption().trim())) {
                    marksObtained += question.getMarks();
                }
            }
        }

        int totalMarks = exam.getQuestions().stream()
                .mapToInt(Question::getMarks)
                .sum();

        result.setMarksObtained(marksObtained);
        result.setTotalMarks(totalMarks);
        result.setSubmitTime(submitTime);
        result.setSubmitted(true);
        result.setTimeExceeded(false);

        return resultRepository.save(result);
    }

    public Result getResultForStudentAndExam(Long studentId, Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with id: " + examId));

        Result result = resultRepository.findByStudentIdAndExamId(studentId, examId)
                .orElseThrow(() -> new IllegalArgumentException("No result found for student " + studentId + " in exam " + examId));

        if (!exam.isResultsPublished()) {
            throw new ResultsNotPublishedException("Results for this exam have not been published yet.");
        }

        return result;
    }

    public List<Result> getResultsForStudent(Long studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotRegisteredException("Student with id " + studentId + " is not registered."));

        List<Result> results = resultRepository.findByStudentId(studentId);
        // Filter out results where exam is not published
        return results.stream()
                .filter(r -> r.getExam().isResultsPublished())
                .collect(Collectors.toList());
    }

    public List<Result> getResultsForExam(Long examId) {
        examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with id: " + examId));
        return resultRepository.findByExamId(examId);
    }
}
