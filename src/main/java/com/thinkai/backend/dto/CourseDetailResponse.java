package com.thinkai.backend.dto;

import lombok.*;
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
