package com.thinkai.backend.dto;

import lombok.*;
<<<<<<< HEAD
=======
import java.math.BigDecimal;
>>>>>>> origin/develop
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
<<<<<<< HEAD
    private Double price;
    private InstructorDto instructor;
    private Boolean isEnrolled;
    private Integer progressPercent;
    private List<LessonSummaryDto> lessons;
=======
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
>>>>>>> origin/develop
}
