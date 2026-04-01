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
public class AdminCourseRequest {

    private String title;
    private String description;
    private BigDecimal price;
    private Long instructorId;
    private String thumbnailUrl;
    private Boolean isPublished;
    private Course.Status status;
}
