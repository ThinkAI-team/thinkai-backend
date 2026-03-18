package com.thinkai.backend.controller;

import com.thinkai.backend.dto.DashboardResponse;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @StudentOnly
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication auth) {
        DashboardResponse dashboard = dashboardService.getDashboard(auth.getName());

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", dashboard));
    }
}
