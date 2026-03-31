package com.thinkai.backend.dto;

import com.thinkai.backend.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseResponse {

    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private BigDecimal price;
    private Long instructorId;
    private Boolean isPublished;
    private Course.Status status;
}
