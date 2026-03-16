package com.thinkai.backend.service;

import com.thinkai.backend.dto.AuthResponse;
import com.thinkai.backend.dto.LoginRequest;
import com.thinkai.backend.dto.RegisterRequest;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .build();
    }
}

