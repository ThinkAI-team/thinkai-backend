package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

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

    // ===================== HELPER =====================

    private Long resolveUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        return user != null ? user.getId() : null;
    }
}
