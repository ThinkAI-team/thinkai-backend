package com.thinkai.backend.service;

import com.thinkai.backend.dto.ReviewRequest;
import com.thinkai.backend.dto.ReviewResponse;
import com.thinkai.backend.entity.CourseReview;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseReviewRepository;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final CourseReviewRepository courseReviewRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    /**
     * Tạo review - chỉ student đã enroll mới được đánh giá
     */
    @Transactional
    public ReviewResponse createReview(Long courseId, Long userId, ReviewRequest request) {
        // Kiểm tra course tồn tại
        if (!courseRepository.existsById(courseId)) {
            throw new ApiException("Không tìm thấy khóa học", HttpStatus.NOT_FOUND);
        }

        // Kiểm tra user đã enroll chưa
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ApiException("Bạn cần đăng ký khóa học trước khi đánh giá", HttpStatus.FORBIDDEN);
        }

        // Kiểm tra đã review chưa
        if (courseReviewRepository.existsByCourseIdAndUserId(courseId, userId)) {
            throw new ApiException("Bạn đã đánh giá khóa học này rồi", HttpStatus.BAD_REQUEST);
        }

        CourseReview review = CourseReview.builder()
                .courseId(courseId)
                .userId(userId)
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .isApproved(true)
                .build();

        review = courseReviewRepository.save(review);
        log.info("Review created: courseId={}, userId={}, rating={}", courseId, userId, request.getRating());

        String userName = getUserName(userId);
        return ReviewResponse.fromEntity(review, userName);
    }

    /**
     * Lấy danh sách review của course (public)
     */
    public List<ReviewResponse> getReviewsByCourseId(Long courseId) {
        List<CourseReview> reviews = courseReviewRepository.findByCourseIdAndIsApprovedTrueOrderByCreatedAtDesc(courseId);

        // Lấy tất cả userIds
        List<Long> userIds = reviews.stream()
                .map(CourseReview::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // Lấy tên user một lần
        Map<Long, String> userNameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));

        return reviews.stream()
                .map(review -> ReviewResponse.fromEntity(review, userNameMap.getOrDefault(review.getUserId(), "Ẩn danh")))
                .collect(Collectors.toList());
    }

    /**
     * Lấy rating trung bình của course
     */
    public Double getAverageRating(Long courseId) {
        List<CourseReview> reviews = courseReviewRepository.findByCourseIdAndIsApprovedTrueOrderByCreatedAtDesc(courseId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(CourseReview::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Kiểm tra user đã review course chưa
     */
    public boolean hasUserReviewed(Long courseId, Long userId) {
        return courseReviewRepository.existsByCourseIdAndUserId(courseId, userId);
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse("Ẩn danh");
    }
}
