package com.thinkai.backend.dto.course;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewResponse {

    private Long id;
    private Long courseId;
    private Long userId;
    private String userFullName;
    private String userAvatar;
    private Integer rating;
    private String reviewText;
    private Boolean isApproved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
