package com.thinkai.backend.service;

import com.thinkai.backend.dto.ProfileResponse;
import com.thinkai.backend.dto.ChangePasswordRequest;
import com.thinkai.backend.dto.UpdateProfileRequest;

import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getProfile(String email) {
        User user = findUser(email);
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUser(email);

        user.setFullName(request.getFullName().trim());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        // 1. Validate confirm
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new ApiException("Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST);
        }

        User user = findUser(email);

        // 2. Nếu đã có mật khẩu thì bắt buộc verify
        if (user.getPasswordHash() != null) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new ApiException("Vui lòng nhập mật khẩu hiện tại", HttpStatus.BAD_REQUEST);
            }
            if (request.getCurrentPassword().equals(request.getNewPassword())) {
                throw new ApiException("Mật khẩu mới phải khác mật khẩu hiện tại", HttpStatus.BAD_REQUEST);
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new ApiException("Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST);
            }
        }

        // 4. Update
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }


    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));
    }

    private ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
