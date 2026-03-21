package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ExamDto;
import com.thinkai.backend.dto.ExamRequest;
import com.thinkai.backend.dto.ExamStartResponse;
import com.thinkai.backend.dto.ExamSubmitRequest;
import com.thinkai.backend.dto.ExamSubmitResponse;
import com.thinkai.backend.dto.ExamHistoryDto;
import com.thinkai.backend.dto.ExamResultResponse;

import com.thinkai.backend.entity.Exam;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.security.TeacherOnly;
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

import java.util.List;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final UserRepository userRepository;

    @TeacherOnly
    @PostMapping
    public ResponseEntity<Exam> createExam(Authentication auth, @Valid @RequestBody ExamRequest request) {
        Long teacherId = requireCurrentUserId(auth);
        Exam exam = examService.createExam(teacherId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(exam);
    }

    @TeacherOnly
    @GetMapping
    public ResponseEntity<Page<Exam>> getAllExams(Authentication auth, Pageable pageable) {
        Long teacherId = requireCurrentUserId(auth);
        Page<Exam> exams = examService.getExamsByTeacher(teacherId, pageable);
        return ResponseEntity.ok(exams);
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
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        Long currentUserId = requireCurrentUserId(auth);
        validateRequestUserId(userId, currentUserId);
        ExamStartResponse response = examService.startExam(examId, currentUserId);
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
            Authentication auth,
            @Valid @RequestBody ExamSubmitRequest request) {
        Long currentUserId = requireCurrentUserId(auth);
        ExamSubmitResponse response = examService.submitExam(examId, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Feature #4: Xem kết quả bài thi.
     * GET /exams/attempts/{attemptId}/result
     */
    @StudentOnly
    @GetMapping("/attempts/{attemptId}/result")
    public ResponseEntity<ExamResultResponse> getExamResult(
            @PathVariable Long attemptId,
            Authentication auth) {
        Long currentUserId = requireCurrentUserId(auth);
        ExamResultResponse response = examService.getExamResult(attemptId, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Feature #5: Lịch sử thi.
     * GET /exams/history?userId={userId}
     */
    @StudentOnly
    @GetMapping("/history")
    public ResponseEntity<List<ExamHistoryDto>> getExamHistory(
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        Long currentUserId = requireCurrentUserId(auth);
        validateRequestUserId(userId, currentUserId);
        List<ExamHistoryDto> history = examService.getExamHistory(currentUserId);
        return ResponseEntity.ok(history);
    }

    private Long requireCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException("Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED);
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return user.getId();
    }

    private void validateRequestUserId(Long requestUserId, Long currentUserId) {
        if (requestUserId != null && !requestUserId.equals(currentUserId)) {
            throw new ApiException("Bạn không có quyền truy cập dữ liệu của user khác", HttpStatus.FORBIDDEN);
        }
    }
}
