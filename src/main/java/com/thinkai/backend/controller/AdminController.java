package com.thinkai.backend.controller;

import com.thinkai.backend.dto.admin.AdminCourseRequest;
import com.thinkai.backend.dto.admin.AdminCourseResponse;
import com.thinkai.backend.dto.common.ApiResponse;
import com.thinkai.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * POST /admin/courses
     * Tạo khóa học mới (chỉ ADMIN)
     */
    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<AdminCourseResponse>> createCourse(
            @Valid @RequestBody AdminCourseRequest request) {
        AdminCourseResponse data = adminService.createCourse(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo khóa học thành công", data));
    }

    /**
     * PUT /admin/courses/{courseId}
     * Cập nhật thông tin khóa học
     */
    @PutMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<AdminCourseResponse>> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody AdminCourseRequest request) {
        AdminCourseResponse data = adminService.updateCourse(courseId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật khóa học thành công", data));
    }

    /**
     * DELETE /admin/courses/{courseId}
     * Xóa khóa học — trả về 204 No Content
     */
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        adminService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }
}
