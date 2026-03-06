package com.thinkai.backend.controller;

import com.thinkai.backend.dto.admin.AdminDashboardResponse;
import com.thinkai.backend.dto.common.ApiResponse;
import com.thinkai.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Toàn bộ /admin/** chỉ ADMIN truy cập được
public class AdminController {

    private final AdminService adminService;

    /**
     * GET /admin/dashboard
     * Thống kê tổng quan hệ thống: users, courses, enrollments, exams, AI chats
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse data = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok("Success", data));
    }
}
