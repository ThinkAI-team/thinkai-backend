package com.thinkai.backend.dto;

import com.thinkai.backend.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long orderCode;
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private Integer amount;
    private String status;
    private String paymentLinkId;
    private String checkoutUrl;
    private String qrCode;
    private String description;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static PaymentResponse fromEntity(Payment payment) {
        return fromEntity(payment, null);
    }

    public static PaymentResponse fromEntity(Payment payment, String courseTitle) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderCode(payment.getOrderCode())
                .userId(payment.getUserId())
                .courseId(payment.getCourseId())
                .courseTitle(courseTitle)
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paymentLinkId(payment.getPaymentLinkId())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCode(payment.getQrCode())
                .description(payment.getDescription())
                .completedAt(payment.getCompletedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
