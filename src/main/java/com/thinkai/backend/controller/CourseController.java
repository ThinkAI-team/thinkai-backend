package com.thinkai.backend.controller;

import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.dto.EnrollmentResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.security.TeacherOnly;
import com.thinkai.backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    /**
     * GET /courses — Danh sách khóa học (Public)
     * Params: keyword, priceMin, priceMax, sortBy, sortDir, page, size
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPublishedCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Map<String, Object> response = courseService.getPublishedCourses(
                keyword, priceMin, priceMax, sortBy, sortDir, page, size
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /courses/{id} — Chi tiết khóa học (Optional auth)
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(
            @PathVariable Long id,
            Authentication auth
    ) {
        Long currentUserId = getCurrentUserId(auth);
        CourseDetailResponse response = courseService.getCourseDetail(id, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /courses/{id}/enroll — Đăng ký khóa học (Student only)
     */
    @StudentOnly
    @PostMapping("/{id}/enroll")
    public ResponseEntity<EnrollmentResponse> enrollCourse(
            @PathVariable Long id,
            Authentication auth
    ) {
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            throw new ApiException("Vui lòng đăng nhập để đăng ký khóa học", HttpStatus.UNAUTHORIZED);
        }
        EnrollmentResponse response = courseService.enrollCourse(id, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ===================== MANAGEMENT ENDPOINTS (TEACHER) =====================

    @TeacherOnly
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        // Chỉ Teacher mới có quyền tạo khóa học
        // TODO: Implement actual logic
        return ResponseEntity.ok(course);
    }

    @TeacherOnly
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        // Chỉ Teacher mới có quyền sửa khóa học
        // TODO: Implement actual logic
        return ResponseEntity.ok(course);
    }

    // ===================== HELPER =====================

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.getId();
        }
        return null;
    }
}

