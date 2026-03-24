package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String instructorName;
    private BigDecimal price;
    private Boolean isEnrolled;
    private int progressPercent;
    private List<LessonResponse> lessons;
}
