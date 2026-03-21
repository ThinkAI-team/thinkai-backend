package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long totalStudents;
    private long totalTeachers;
    private long totalAdmins;
    private long totalCourses;
    private long totalPublishedCourses;
    private long totalEnrollments;
    private long totalExams;
}
