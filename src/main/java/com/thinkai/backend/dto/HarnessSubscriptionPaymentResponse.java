package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarnessSubscriptionPaymentResponse {
    private Long orderCode;
    private Integer amount;
    private String description;
    private String paymentLinkId;
    private String checkoutUrl;
    private String qrCode;
    private String status;
}
