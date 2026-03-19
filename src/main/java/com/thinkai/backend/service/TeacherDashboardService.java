package com.thinkai.backend.service;

import com.thinkai.backend.dto.DashboardStatsResponse;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherDashboardService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long teacherId) {
        long totalCourses = courseRepository.countByInstructorId(teacherId);
        long totalStudents = enrollmentRepository.countEnrolledStudentsByInstructorId(teacherId);
        long completedStudents = enrollmentRepository.countCompletedStudentsByInstructorId(teacherId);
        
        double completionRate = totalStudents == 0 ? 0 : 
                               (double) completedStudents / totalStudents * 100;
        
        return DashboardStatsResponse.builder()
                .totalCourses(totalCourses)
                .totalStudents(totalStudents)
                .completedStudents(completedStudents)
                .completionRate(completionRate)
                .build();
    }
}
