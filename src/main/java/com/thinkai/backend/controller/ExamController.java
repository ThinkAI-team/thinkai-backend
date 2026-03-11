package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // 👇 Gắn annotation theo SECURITY_GUIDE
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{courseId}/exams")
    public ResponseEntity<List<ExamDto>> getExamsByCourse(@PathVariable Long courseId) {
        List<ExamDto> exams = examService.getExamsByCourseId(courseId);
        return ResponseEntity.ok(exams);
    }
}
