package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.HarnessSubscriptionPaymentRequest;
import com.thinkai.backend.dto.HarnessSubscriptionPaymentResponse;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.HarnessSubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/harness-subscriptions")
@RequiredArgsConstructor
public class HarnessSubscriptionController {

    private final HarnessSubscriptionPaymentService harnessSubscriptionPaymentService;
    private final UserRepository userRepository;

    @StudentOnly
    @PostMapping("/create-payment-link")
    public ResponseEntity<ApiResponse<HarnessSubscriptionPaymentResponse>> createPaymentLink(
            @RequestBody(required = false) HarnessSubscriptionPaymentRequest request,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        HarnessSubscriptionPaymentResponse response = harnessSubscriptionPaymentService.createPaymentLink(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Tạo link thanh toán Harness thành công", response));
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException("Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED);
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new ApiException("Không tìm thấy user", HttpStatus.NOT_FOUND);
        }
        return user.getId();
    }
}
