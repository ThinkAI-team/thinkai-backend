package com.thinkai.backend.service;

import com.thinkai.backend.dto.admin.AdminDashboardResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.AiChatLogRepository;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.EnrollmentRepository;
import com.thinkai.backend.repository.ExamAttemptRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AiChatLogRepository aiChatLogRepository;

    /**
     * Lấy thống kê tổng quan hệ thống cho Admin Dashboard
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStats() {

        // Đếm học viên & giảng viên đang active
        long totalStudents = userRepository.countByRoleAndIsActive(User.Role.STUDENT, true);
        long totalTeachers = userRepository.countByRoleAndIsActive(User.Role.TEACHER, true);

        // Đếm khóa học
        long publishedCourses = courseRepository.countByIsPublished(true);
        long pendingCourses = courseRepository.countByStatus(Course.Status.PENDING);

        // Đếm enrollments & exam attempts
        long totalEnrollments = enrollmentRepository.count();
        long totalExamAttempts = examAttemptRepository.count();

        // Đếm AI chat hôm nay (00:00 → 23:59)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long aiChatsToday = aiChatLogRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        return AdminDashboardResponse.builder()
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .publishedCourses(publishedCourses)
                .pendingCourses(pendingCourses)
                .totalEnrollments(totalEnrollments)
                .totalExamAttempts(totalExamAttempts)
                .aiChatsToday(aiChatsToday)
                .build();
    }
}
