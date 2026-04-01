package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOSWebhookRequest {

    private String code;
    private String desc;
    private Boolean success;
    private PayOSWebhookData data;
    private String signature;
}
