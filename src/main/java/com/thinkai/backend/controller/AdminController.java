package com.thinkai.backend.controller;

import com.thinkai.backend.dto.admin.AdminUserResponse;
import com.thinkai.backend.dto.admin.UpdateUserStatusRequest;
import com.thinkai.backend.dto.common.ApiResponse;
import com.thinkai.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
     * GET /admin/users?page=0&size=10&keyword=...&role=STUDENT&isActive=true
     * Lấy danh sách tất cả users với filter và phân trang
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive) {

        Page<AdminUserResponse> data = adminService.getUsers(page, size, keyword, role, isActive);
        return ResponseEntity.ok(ApiResponse.ok("Success", data));
    }

    /**
     * PUT /admin/users/{userId}/status
     * Lock hoặc Unlock tài khoản user
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        AdminUserResponse data = adminService.updateUserStatus(userId, request);
        String message = request.getIsActive() ? "Đã kích hoạt tài khoản" : "Đã khóa tài khoản";
        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }
}
