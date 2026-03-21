package com.thinkai.backend.service;

import com.thinkai.backend.dto.AdminStatsResponse;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.ExamRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamRepository examRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalStudents(userRepository.countByRole(User.Role.STUDENT))
                .totalTeachers(userRepository.countByRole(User.Role.TEACHER))
                .totalAdmins(userRepository.countByRole(User.Role.ADMIN))
                .totalCourses(courseRepository.count())
                .totalPublishedCourses(courseRepository.countByIsPublishedTrue())
                .totalEnrollments(enrollmentRepository.count())
                .totalExams(examRepository.count())
                .build();
    }
}
