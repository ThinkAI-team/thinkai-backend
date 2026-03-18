package com.thinkai.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.thinkai.backend.dto.AuthResponse;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${google.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        if (googleClientId != null && !googleClientId.isBlank()) {
            verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        }
    }

    @Transactional
    public AuthResponse loginWithGoogle(String idTokenString) {
        if (verifier == null) {
            throw new ApiException(
                    "Google OAuth chưa được cấu hình. Vui lòng liên hệ admin.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        // 1. Verify ID token với Google
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (Exception e) {
            throw new ApiException("Không thể xác thực Google token", HttpStatus.UNAUTHORIZED);
        }

        if (idToken == null) {
            throw new ApiException("Google token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED);
        }

        // 2. Lấy thông tin user từ token
        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture");

        // 3. Tìm hoặc tạo user
        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    // Check nếu email đã tồn tại (đăng ký bằng email trước đó)
                    User existingUser = userRepository.findByEmail(email).orElse(null);
                    if (existingUser != null) {
                        // Liên kết Google vào tài khoản hiện có
                        existingUser.setGoogleId(googleId);
                        if (existingUser.getAvatarUrl() == null && avatarUrl != null) {
                            existingUser.setAvatarUrl(avatarUrl);
                        }
                        return userRepository.save(existingUser);
                    }

                    // Tạo user mới
                    User newUser = User.builder()
                            .email(email)
                            .googleId(googleId)
                            .fullName(name != null ? name : email.split("@")[0])
                            .avatarUrl(avatarUrl)
                            .role(User.Role.STUDENT)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });

        // 4. Check tài khoản bị khóa
        if (!user.getIsActive()) {
            throw new ApiException("Tài khoản đã bị khóa", HttpStatus.FORBIDDEN);
        }

        // 5. Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), Map.of(
                "role", user.getRole().name(),
                "fullName", user.getFullName()
        ));

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .hasPassword(user.getPasswordHash() != null)
                .isGoogleUser(user.getGoogleId() != null)
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
