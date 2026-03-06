package com.thinkai.backend.controller;

import com.thinkai.backend.dto.common.ApiResponse;
import com.thinkai.backend.dto.course.CourseReviewRequest;
import com.thinkai.backend.dto.course.CourseReviewResponse;
import com.thinkai.backend.service.CourseReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService reviewService;

    /**
     * GET /courses/{courseId}/reviews
     * Lấy danh sách đánh giá của khóa học (public, ai cũng xem được)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourseReviewResponse>>> getReviews(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CourseReviewResponse> data = reviewService.getReviews(courseId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đánh giá thành công", data));
    }

    /**
     * POST /courses/{courseId}/reviews
     * Thêm hoặc cập nhật đánh giá (Yêu cầu đăng nhập, STUDENT hoặc mọi role)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CourseReviewResponse>> createReview(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseReviewRequest request) {

        CourseReviewResponse data = reviewService.createReview(courseId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng đánh giá thành công", data));
    }
}
