package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ExamRequest;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/exams")
@RequiredArgsConstructor
public class TeacherExamController {

    private final ExamService examService;
    private final UserRepository userRepository;

    private Long getTeacherId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .map(User::getId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Exam> createExam(Authentication auth, @Valid @RequestBody ExamRequest request) {
        Exam exam = examService.createExam(getTeacherId(auth), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(exam);
    }

    @GetMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Page<Exam>> getExams(Authentication auth, Pageable pageable) {
        Page<Exam> exams = examService.getExamsByTeacher(getTeacherId(auth), pageable);
        return ResponseEntity.ok(exams);
    }
}
