package com.thinkai.backend.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetailResponse {

    private Long id;
    private String title;
    private String description;
    private String thumbnail;
    private BigDecimal price;
    private InstructorInfo instructor;
    private Boolean isEnrolled;
    private Integer progressPercent;
    private List<LessonResponse> lessons;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InstructorInfo {
        private Long id;
        private String fullName;
        private String avatarUrl;
    }
}
