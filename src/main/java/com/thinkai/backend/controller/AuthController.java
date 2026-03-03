package com.thinkai.backend.controller;

import com.thinkai.backend.dto.AuthResponse;
import com.thinkai.backend.dto.ForgotPasswordRequest;
import com.thinkai.backend.dto.LoginRequest;
import com.thinkai.backend.dto.RegisterRequest;
import com.thinkai.backend.dto.ResetPasswordRequest;
import com.thinkai.backend.service.AuthService;
import com.thinkai.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.ok(Map.of(
                "message", "Nếu email tồn tại, chúng tôi đã gửi link đặt lại mật khẩu."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập."));
    }
}
