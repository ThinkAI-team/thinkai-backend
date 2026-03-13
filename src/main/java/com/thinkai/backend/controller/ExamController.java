package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.dto.ExamResultResponse;
import com.thinkai.backend.dto.ExamStartResponse;
import com.thinkai.backend.dto.ExamSubmitRequest;
import com.thinkai.backend.dto.ExamSubmitResponse;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    /**
     * Feature #1: Lấy danh sách bài thi của một khóa học.
     * GET /courses/{courseId}/exams
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/courses/{courseId}/exams")
    public ResponseEntity<List<ExamDto>> getExamsByCourse(@PathVariable Long courseId) {
        List<ExamDto> exams = examService.getExamsByCourseId(courseId);
        return ResponseEntity.ok(exams);
    }

    /**
     * Feature #2: Bắt đầu làm bài thi.
     * POST /exams/{examId}/start
     */
    @StudentOnly
    @PostMapping("/exams/{examId}/start")
    public ResponseEntity<ExamStartResponse> startExam(
            @PathVariable Long examId,
            @RequestParam Long userId) {
        ExamStartResponse response = examService.startExam(examId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Feature #3: Nộp bài thi.
     * POST /exams/{examId}/submit
     */
    @StudentOnly
    @PostMapping("/exams/{examId}/submit")
    public ResponseEntity<ExamSubmitResponse> submitExam(
            @PathVariable Long examId,
            @Valid @RequestBody ExamSubmitRequest request) {
        ExamSubmitResponse response = examService.submitExam(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Feature #4: Xem kết quả bài thi.
     * GET /exams/attempts/{attemptId}/result
     */
    @StudentOnly
    @GetMapping("/exams/attempts/{attemptId}/result")
    public ResponseEntity<ExamResultResponse> getExamResult(@PathVariable Long attemptId) {
        ExamResultResponse response = examService.getExamResult(attemptId);
        return ResponseEntity.ok(response);
    }
}
