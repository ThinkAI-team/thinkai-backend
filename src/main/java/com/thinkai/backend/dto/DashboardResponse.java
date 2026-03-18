package com.thinkai.backend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private String greeting;
    private int totalEnrolledCourses;
    private double averageProgress;
    private List<EnrolledCourseDto> enrolledCourses;
    private NextLessonDto nextLesson;
}
