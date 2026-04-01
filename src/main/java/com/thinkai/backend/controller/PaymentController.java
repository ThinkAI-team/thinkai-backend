package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.PaymentRequest;
import com.thinkai.backend.dto.PaymentResponse;
import com.thinkai.backend.dto.PayOSWebhookRequest;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @StudentOnly
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestBody PaymentRequest request,
            Authentication auth) {
        
        Long userId = getCurrentUserId(auth);
        PaymentResponse response = paymentService.createPaymentLink(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Tạo link thanh toán thành công", response));
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long orderCode) {
        
        PaymentResponse response = paymentService.getPaymentByOrderCode(orderCode);
        
        return ResponseEntity.ok(ApiResponse.success("Thông tin thanh toán", response));
    }

    /**
     * Endpoint confirm thanh toán - frontend gọi sau khi PayOS redirect về với status=PAID
     */
    @StudentOnly
    @PostMapping("/confirm/{orderCode}")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable Long orderCode,
            Authentication auth) {
        
        Long userId = getCurrentUserId(auth);
        PaymentResponse response = paymentService.confirmPayment(orderCode, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán thành công", response));
    }

    /**
     * Webhook PayOS - luôn trả 200 OK
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody(required = false) PayOSWebhookRequest request) {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/webhook-test")
    public ResponseEntity<String> webhookTest() {
        return ResponseEntity.ok("Webhook URL is working!");
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException("Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED);
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.getId();
        }
        throw new ApiException("Không tìm thấy user", HttpStatus.NOT_FOUND);
    }
}
