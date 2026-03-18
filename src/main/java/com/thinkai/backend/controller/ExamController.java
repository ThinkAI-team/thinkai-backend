package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.dto.ExamStartResponse;
import com.thinkai.backend.dto.ExamSubmitRequest;
import com.thinkai.backend.dto.ExamSubmitResponse;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.security.TeacherOnly;
import com.thinkai.backend.service.ExamService;
import jakarta.validation.Valid;
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

    @GetMapping
    public ResponseEntity<List<Exam>> getAllExams() {
        return ResponseEntity.ok(List.of());
    }

    /**
     * Feature #1: Lấy danh sách bài thi của một khóa học.
     * GET /exams/{courseId}/exams
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{courseId}/exams")
    public ResponseEntity<List<ExamDto>> getExamsByCourse(@PathVariable Long courseId) {
        List<ExamDto> exams = examService.getExamsByCourseId(courseId);
        return ResponseEntity.ok(exams);
    }

    /**
     * Feature #2: Bắt đầu làm bài thi.
     * POST /exams/{examId}/start
     */
    @StudentOnly
    @PostMapping("/{examId}/start")
    public ResponseEntity<ExamStartResponse> startExam(
            @PathVariable Long examId,
            @RequestParam Long userId) {
        ExamStartResponse response = examService.startExam(examId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Feature #3: Nộp bài thi.
     * POST /exams/{examId}/submit
     *
     * @param examId  ID của bài thi (dùng để xác định context)
     * @param request Body chứa attemptId + danh sách câu trả lời
     * @return ExamSubmitResponse chứa kết quả chấm điểm
     */
    @StudentOnly
    @PostMapping("/{examId}/submit")
    public ResponseEntity<ExamSubmitResponse> submitExam(
            @PathVariable Long examId,
            @Valid @RequestBody ExamSubmitRequest request) {
        ExamSubmitResponse response = examService.submitExam(request);
        return ResponseEntity.ok(response);
    }
}
