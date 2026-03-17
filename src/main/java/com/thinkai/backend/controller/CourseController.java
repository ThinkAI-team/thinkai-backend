package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.security.TeacherOnly;
import com.thinkai.backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    /**
     * GET /courses — Danh sách khóa học (Public)
     */
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        // TODO: Implement actual logic in CourseService
        return ResponseEntity.ok(List.of());
    }

    /**
     * GET /courses/{courseId} — Chi tiết khóa học (Optional auth)
     *
     * - Không cần token: trả detail + lessons, isEnrolled = false
     * - Có token: trả thêm isEnrolled + progressPercent thật
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourseDetail(
            @PathVariable Long courseId,
            Authentication auth
    ) {
        Long currentUserId = resolveUserId(auth);
        CourseDetailResponse detail = courseService.getCourseDetail(courseId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @TeacherOnly
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        // Chỉ Teacher mới có quyền tạo khóa học
        return ResponseEntity.ok(course);
    }

    @TeacherOnly
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        // Chỉ Teacher mới có quyền sửa khóa học
        return ResponseEntity.ok(course);
    }

    // ===================== HELPER =====================

    private Long resolveUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        return user != null ? user.getId() : null;
    }
}

