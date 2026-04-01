package com.thinkai.backend.dto;

import com.thinkai.backend.entity.CourseReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long courseId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String reviewText;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(CourseReview review, String userName) {
        return ReviewResponse.builder()
                .id(review.getId())
                .courseId(review.getCourseId())
                .userId(review.getUserId())
                .userName(userName)
                .rating(review.getRating())
                .reviewText(review.getReviewText())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
