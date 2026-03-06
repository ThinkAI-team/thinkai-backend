package com.thinkai.backend.service;

import com.thinkai.backend.dto.course.CourseReviewRequest;
import com.thinkai.backend.dto.course.CourseReviewResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.CourseReview;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.CourseReviewRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository; // Cần dùng để map tên ng dùng, hoặc Security auth

    @Transactional
    public CourseReviewResponse createReview(Long courseId, CourseReviewRequest request) {
        if (!courseRepository.existsById(courseId)) {
            throw new ApiException("Không tìm thấy khóa học", HttpStatus.NOT_FOUND);
        }

        User user = getCurrentUser();
        if (user == null) {
            throw new ApiException("User không hợp lệ", HttpStatus.UNAUTHORIZED);
        }

        // Mỗi user chỉ dc review 1 lần cho 1 khóa học (duy trì constraint)
        Optional<CourseReview> existingReview = reviewRepository.findByCourseIdAndUserId(courseId, user.getId());
        CourseReview review;

        if (existingReview.isPresent()) {
            // Update
            review = existingReview.get();
            review.setRating(request.getRating());
            review.setReviewText(request.getReviewText());
            // Admin có thể set isApproved = false nếu cần kiểm duyệt lại, ở đây mặc định
            // giữ nguyên
        } else {
            // Create new
            review = CourseReview.builder()
                    .courseId(courseId)
                    .userId(user.getId())
                    .rating(request.getRating())
                    .reviewText(request.getReviewText())
                    .isApproved(true) // Mặc định tự động duyệt, admin có thể tắt sau
                    .build();
        }

        review = reviewRepository.save(review);
        return toReviewResponse(review, user);
    }

    @Transactional(readOnly = true)
    public Page<CourseReviewResponse> getReviews(Long courseId, int page, int size) {
        if (!courseRepository.existsById(courseId)) {
            throw new ApiException("Không tìm thấy khóa học", HttpStatus.NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Trả về các review đã duyệt
        return reviewRepository.findByCourseIdAndIsApproved(courseId, true, pageable)
                .map(review -> {
                    // Trong thực tế, có thể join bảng thay vì fecth riêng lẻ,
                    // nhưng với JpaRepository đây là phương án đơn giản, sẽ có N+1 nếu không cẩn
                    // thận.
                    // Tối ưu: Dùng @Query JOIN FETCH User.
                    User user = userRepository.findById(review.getUserId()).orElse(null);
                    return toReviewResponse(review, user);
                });
    }

    private CourseReviewResponse toReviewResponse(CourseReview review, User user) {
        return CourseReviewResponse.builder()
                .id(review.getId())
                .courseId(review.getCourseId())
                .userId(review.getUserId())
                .userFullName(user != null ? user.getFullName() : "Unknown")
                .userAvatar(user != null ? user.getAvatarUrl() : null)
                .rating(review.getRating())
                .reviewText(review.getReviewText())
                .isApproved(review.getIsApproved())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            String email = auth.getName(); // assuming username is email
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }
}
