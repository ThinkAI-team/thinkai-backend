package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.ReviewRequest;
import com.thinkai.backend.dto.ReviewResponse;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    /**
     * GET /courses/{courseId}/reviews - Lấy danh sách review (public)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviews(@PathVariable Long courseId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByCourseId(courseId);
        Double averageRating = reviewService.getAverageRating(courseId);

        Map<String, Object> data = new HashMap<>();
        data.put("reviews", reviews);
        data.put("averageRating", Math.round(averageRating * 10.0) / 10.0);
        data.put("totalReviews", reviews.size());

        return ResponseEntity.ok(ApiResponse.success("Danh sách đánh giá", data));
    }

    /**
     * POST /courses/{courseId}/reviews - Tạo review (student đã enroll)
     */
    @StudentOnly
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long courseId,
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {

        Long userId = getCurrentUserId(auth);
        ReviewResponse response = reviewService.createReview(courseId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá thành công", response));
    }

    /**
     * GET /courses/{courseId}/reviews/check - Kiểm tra user đã review chưa
     */
    @StudentOnly
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkReview(
            @PathVariable Long courseId,
            Authentication auth) {

        Long userId = getCurrentUserId(auth);
        boolean hasReviewed = reviewService.hasUserReviewed(courseId, userId);

        Map<String, Boolean> data = new HashMap<>();
        data.put("hasReviewed", hasReviewed);

        return ResponseEntity.ok(ApiResponse.success("Trạng thái đánh giá", data));
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException("Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED);
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.getId();
        }
        throw new ApiException("Không tìm thấy user", HttpStatus.NOT_FOUND);
    }
}
