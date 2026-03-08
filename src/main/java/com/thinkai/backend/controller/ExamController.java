package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.dto.ExamStartResponse;
import com.thinkai.backend.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    @GetMapping("/courses/{courseId}/exams")
    public ResponseEntity<List<ExamDto>> getExamsByCourse(@PathVariable Long courseId) {
        List<ExamDto> exams = examService.getExamsByCourseId(courseId);
        return ResponseEntity.ok(exams);
    }

    /**
     * Feature #2: Bắt đầu làm bài thi.
     * POST /exams/{examId}/start
     *
     * @param examId ID của bài thi
     * @param userId ID của user (tạm thời truyền qua param, sau sẽ lấy từ JWT)
     * @return ExamStartResponse chứa thông tin phiên thi và danh sách câu hỏi
     */
    @PostMapping("/exams/{examId}/start")
    public ResponseEntity<ExamStartResponse> startExam(
            @PathVariable Long examId,
            @RequestParam Long userId) {
        ExamStartResponse response = examService.startExam(examId, userId);
        return ResponseEntity.ok(response);
    }
}
