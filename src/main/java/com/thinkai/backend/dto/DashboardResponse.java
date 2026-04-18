package com.thinkai.backend.dto;

import lombok.*;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String greeting;
    private int totalEnrolledCourses;
    private double averageProgress;
    private List<EnrolledCourseDto> enrolledCourses;
    private NextLessonDto nextLesson;
}
