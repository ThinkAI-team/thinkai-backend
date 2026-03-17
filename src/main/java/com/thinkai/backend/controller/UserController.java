package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.ChangePasswordRequest;
import com.thinkai.backend.dto.MyCourseResponse;
import com.thinkai.backend.dto.ProfileResponse;
import com.thinkai.backend.dto.UpdateProfileRequest;
import com.thinkai.backend.service.CourseService;
import com.thinkai.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CourseService courseService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getProfile(Authentication auth) {
        ProfileResponse profile = userService.getProfile(auth.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse profile = userService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(auth.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    /**
     * GET /users/me/courses — Danh sách khóa học đã đăng ký
     * Auth: Bearer Token (bắt buộc)
     */
    @GetMapping("/me/courses")
    public ResponseEntity<ApiResponse<List<MyCourseResponse>>> getMyCourses(Authentication auth) {
        List<MyCourseResponse> courses = courseService.getMyCourses(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(courses));
    }
}

