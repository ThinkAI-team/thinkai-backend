package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.DashboardStatsResponse;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.TeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/teacher", "/teacher-portal"})
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final TeacherDashboardService dashboardService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboard(Authentication auth) {
        User teacher = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
                
        DashboardStatsResponse stats = dashboardService.getDashboardStats(teacher.getId());
        return ResponseEntity.ok(ApiResponse.success("Teacher dashboard stats", stats));
    }
}
