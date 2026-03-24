package com.thinkai.backend.controller;

import com.thinkai.backend.dto.AdminStatsResponse;
import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.security.AdminOnly;
import com.thinkai.backend.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/admin", "/admin-panel"})
@RequiredArgsConstructor
public class AdminPanelController {

    private final AdminStatsService adminStatsService;

    @AdminOnly
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminStatsService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Admin system stats", stats));
    }
}
