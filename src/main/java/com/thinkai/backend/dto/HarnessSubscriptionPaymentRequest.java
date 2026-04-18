package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarnessSubscriptionPaymentRequest {
    private String planCode;
    private Integer amountVnd;
    private String returnUrl;
    private String cancelUrl;
}
