package com.thinkai.backend.service;

import com.thinkai.backend.dto.ForgotPasswordRequest;
import com.thinkai.backend.dto.ResetPasswordRequest;
import com.thinkai.backend.entity.PasswordResetToken;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.PasswordResetTokenRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int TOKEN_EXPIRY_MINUTES = 30;
    private static final int MAX_REQUESTS_PER_HOUR = 3;

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Always return success to prevent email enumeration
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        // Rate limit: max 3 requests per hour
        long recentRequests = tokenRepository.countByUserAndCreatedAtAfter(
                user, LocalDateTime.now().minusHours(1));
        if (recentRequests >= MAX_REQUESTS_PER_HOUR) {
            throw new ApiException(
                    "Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau 1 giờ.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        // Create token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // Send email
        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Validate confirm password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST);
        }

        // Find and validate token
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ApiException(
                        "Link đặt lại mật khẩu không hợp lệ", HttpStatus.BAD_REQUEST));

        if (resetToken.getUsed()) {
            throw new ApiException(
                    "Link đặt lại mật khẩu đã được sử dụng", HttpStatus.BAD_REQUEST);
        }

        if (resetToken.isExpired()) {
            throw new ApiException(
                    "Link đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại.",
                    HttpStatus.BAD_REQUEST);
        }

        // Update password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
