package com.thinkai.backend.controller;

import com.thinkai.backend.dto.*;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.CourseService;
import com.thinkai.backend.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final LessonService lessonService;

    /**
     * GET /courses/{courseId} — Chi tiết khóa học + danh sách bài cho sidebar.
     * Auth: Optional (nếu đã login → trả thêm isEnrolled, progress).
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<Map<String, Object>> getCourseDetail(
            @PathVariable Long courseId,
            Authentication auth) {

        String email = (auth != null) ? auth.getName() : null;
        CourseDetailResponse detail = courseService.getCourseDetail(courseId, email);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", detail));
    }

    /**
     * POST /courses/{courseId}/enroll — Đăng ký khóa học.
     * Auth: STUDENT only.
     */
    @StudentOnly
    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<Map<String, Object>> enrollCourse(
            @PathVariable Long courseId,
            Authentication auth) {

        EnrollResponse response = courseService.enrollCourse(courseId, auth.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", 201,
                "message", "Enrolled successfully",
                "data", response));
    }

    /**
     * GET /courses/lessons/{lessonId} — Nội dung bài học (video/PDF).
     * Auth: Bearer Token (phải đã enroll khóa học).
     */
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<Map<String, Object>> getLessonDetail(
            @PathVariable Long lessonId,
            Authentication auth) {

        LessonDetailResponse detail = lessonService.getLessonDetail(lessonId, auth.getName());

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", detail));
    }

    /**
     * POST /courses/lessons/{lessonId}/complete — Đánh dấu hoàn thành bài học.
     * Auth: STUDENT only.
     */
    @StudentOnly
    @PostMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<Map<String, Object>> completeLesson(
            @PathVariable Long lessonId,
            Authentication auth,
            @Valid @RequestBody LessonCompleteRequest request) {

        LessonCompleteResponse response = lessonService.completeLesson(
                lessonId, auth.getName(), request);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Progress updated",
                "data", response));
    }
}
