package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.security.TeacherOnly;
import com.thinkai.backend.service.ExamService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @TeacherOnly
    @PostMapping
    public ResponseEntity<Exam> createExam(@RequestBody Exam exam) {
        // Chỉ Teacher mới được tạo đề
        return ResponseEntity.ok(exam);
    }

    @StudentOnly
    @PostMapping("/{id}/submit")
    public ResponseEntity<String> submitExam(@PathVariable Long id) {
        // Chỉ Student mới được làm bài/nộp bài
        return ResponseEntity.ok("Exam submitted");
    }

    @GetMapping
    public ResponseEntity<List<Exam>> getAllExams() {
        return ResponseEntity.ok(List.of());
    }

    // 👇 Gắn annotation theo SECURITY_GUIDE
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{courseId}/exams")
    public ResponseEntity<List<ExamDto>> getExamsByCourse(@PathVariable Long courseId) {
        List<ExamDto> exams = examService.getExamsByCourseId(courseId);
        return ResponseEntity.ok(exams);
    }
}
