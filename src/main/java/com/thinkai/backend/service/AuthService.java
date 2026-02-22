package com.thinkai.backend.service;

import com.thinkai.backend.dto.AuthResponse;
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

        // 2. Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email đã được sử dụng", HttpStatus.CONFLICT);
        }

        // 3. Build and save user
        String fullName = request.getFirstName().trim() + " " + request.getLastName().trim();

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(fullName)
                .role(User.Role.STUDENT)
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
}
