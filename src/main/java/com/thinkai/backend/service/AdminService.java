package com.thinkai.backend.service;

import com.thinkai.backend.dto.admin.AdminUserResponse;
import com.thinkai.backend.dto.admin.UpdateUserStatusRequest;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    /**
     * GET /admin/users — Lấy danh sách users với filter + phân trang
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(
            int page, int size,
            String keyword,
            String roleStr,
            Boolean isActive) {

        // Parse role string → enum (null nếu không truyền)
        User.Role role = null;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = User.Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Role không hợp lệ. Giá trị hợp lệ: STUDENT, TEACHER, ADMIN", HttpStatus.BAD_REQUEST);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return userRepository
                .searchUsers(keyword, role, isActive, pageable)
                .map(this::toAdminUserResponse);
    }

    /**
     * PUT /admin/users/{userId}/status — Lock/Unlock tài khoản
     */
    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Không tìm thấy user với id: " + userId, HttpStatus.NOT_FOUND));

        // Không cho phép lock chính mình (phòng tình huống admin tự khóa)
        user.setIsActive(request.getIsActive());
        userRepository.save(user);

        return toAdminUserResponse(user);
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────
    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
