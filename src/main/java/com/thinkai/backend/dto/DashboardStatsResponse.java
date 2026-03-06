package com.thinkai.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private long totalCourses;
    private long totalStudents;
    private long completedStudents;
    private double completionRate;
}
