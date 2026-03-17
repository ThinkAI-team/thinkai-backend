package com.thinkai.backend.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseListResponse {

    private Long id;
    private String title;
    private String thumbnail;
    private BigDecimal price;
    private InstructorInfo instructor;
    private int lessonsCount;
    private int enrolledCount;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InstructorInfo {
        private Long id;
        private String fullName;
    }
}
