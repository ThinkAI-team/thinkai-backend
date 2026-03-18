package com.thinkai.backend.service;

import com.thinkai.backend.dto.AuthResponse;
import com.thinkai.backend.dto.LoginRequest;
import com.thinkai.backend.dto.RegisterRequest;
import com.thinkai.backend.dto.UpdatePasswordRequest;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Validate confirm password
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST);
        }

        // 2. Normalize email
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 3. Check duplicate email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException("Email đã được sử dụng", HttpStatus.CONFLICT);
        }

        // 3. Build and save user
        String fullName = request.getFirstName().trim() + " " + request.getLastName().trim();

        // 4. Parse role (chỉ cho phép STUDENT hoặc TEACHER, mặc định STUDENT)
        User.Role userRole = User.Role.STUDENT;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                User.Role requestedRole = User.Role.valueOf(request.getRole().trim().toUpperCase());
                if (requestedRole == User.Role.ADMIN) {
                    throw new ApiException("Không được phép đăng ký với vai trò ADMIN", HttpStatus.FORBIDDEN);
                }
                userRole = requestedRole;
            } catch (IllegalArgumentException e) {
                throw new ApiException("Vai trò không hợp lệ. Chỉ chấp nhận: STUDENT, TEACHER", HttpStatus.BAD_REQUEST);
            }
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(fullName)
                .role(userRole)
                .isActive(true)
                .build();

        userRepository.save(user);

        // 4. Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), Map.of(
                "role", user.getRole().name(),
                "fullName", user.getFullName()
        ));

        // 5. Return response
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .hasPassword(user.getPasswordHash() != null)
                .isGoogleUser(user.getGoogleId() != null)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Find user by email (generic error for security)
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(
                        "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED));

        // 2. Check account active
        if (!user.getIsActive()) {
            throw new ApiException("Tài khoản đã bị khóa", HttpStatus.FORBIDDEN);
        }

        // 3. Verify password (same generic error)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException("Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED);
        }

        // 4. Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), Map.of(
                "role", user.getRole().name(),
                "fullName", user.getFullName()
        ));

        // 5. Return response
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .hasPassword(user.getPasswordHash() != null)
                .isGoogleUser(user.getGoogleId() != null)
                .build();
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        // 1. Nếu đã có mật khẩu, yêu cầu phải nhập đúng mật khẩu cũ
        if (user.getPasswordHash() != null) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new ApiException("Vui lòng nhập mật khẩu hiện tại", HttpStatus.BAD_REQUEST);
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new ApiException("Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST);
            }
        }

        // 2. Kiểm tra mật khẩu mới và xác nhận
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST);
        }

        // 3. Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        return AuthResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .hasPassword(user.getPasswordHash() != null)
                .isGoogleUser(user.getGoogleId() != null)
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}

