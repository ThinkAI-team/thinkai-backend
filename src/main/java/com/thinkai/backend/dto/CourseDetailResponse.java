package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Double price;
    private InstructorDto instructor;
    private Boolean isEnrolled;
    private Integer progressPercent;
    private List<LessonSummaryDto> lessons;
}
