package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.EnrollmentResponse;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * POST /courses/{courseId}/enroll — Đăng ký khóa học
     * Auth: Bearer Token, role STUDENT
     * Response: 201 Created
     */
    @PostMapping("/{courseId}/enroll")
    @StudentOnly
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollCourse(
            @PathVariable Long courseId,
            Authentication auth
    ) {
        String email = auth.getName();
        EnrollmentResponse response = courseService.enrollCourse(courseId, email);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Enrolled successfully", response));
    }
}
