package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.dto.ExamStartResponse;
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

    /**
     * Feature #2: Bắt đầu làm bài thi.
     * POST /exams/{examId}/start
     *
     * @param examId ID của bài thi
     * @param userId ID của user (tạm thời truyền qua param, sau sẽ lấy từ JWT)
     * @return ExamStartResponse chứa thông tin phiên thi và danh sách câu hỏi
     */
    @StudentOnly
    @PostMapping("/{examId}/start")
    public ResponseEntity<ExamStartResponse> startExam(
            @PathVariable Long examId,
            @RequestParam Long userId) {
        ExamStartResponse response = examService.startExam(examId, userId);
        return ResponseEntity.ok(response);
    }
}
