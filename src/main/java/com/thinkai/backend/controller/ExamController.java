package com.thinkai.backend.controller;

import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.security.TeacherOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

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
}
