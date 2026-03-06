package com.thinkai.backend.dto.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {

    private long totalStudents; // Tổng học viên đang hoạt động
    private long totalTeachers; // Tổng giảng viên đang hoạt động
    private long publishedCourses; // Số khóa học đã xuất bản
    private long pendingCourses; // Số khóa học chờ duyệt
    private long totalEnrollments; // Tổng lượt đăng ký khóa học
    private long totalExamAttempts; // Tổng lượt làm bài thi
    private long aiChatsToday; // Số lượt chat AI hôm nay
}
