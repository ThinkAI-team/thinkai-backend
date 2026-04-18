package com.thinkai.backend.service;

import com.thinkai.backend.config.PayOSConfig;
import com.thinkai.backend.dto.HarnessSubscriptionPaymentRequest;
import com.thinkai.backend.dto.HarnessSubscriptionPaymentResponse;
import com.thinkai.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HarnessSubscriptionPaymentService {

    private static final int DEFAULT_MONTHLY_PRICE_VND = 26000;
    private static final String DEFAULT_PLAN_CODE = "harness-monthly";

    private final PayOSConfig payOSConfig;

    @Transactional
    public HarnessSubscriptionPaymentResponse createPaymentLink(Long userId, HarnessSubscriptionPaymentRequest request) {
        Long orderCode = System.currentTimeMillis();
        int amount = resolveAmount(request != null ? request.getAmountVnd() : null);
        String planCode = sanitizePlanCode(request != null ? request.getPlanCode() : null);
        String description = "Harness " + shortPlan(planCode);
        String returnUrl = resolveUrl(request != null ? request.getReturnUrl() : null, payOSConfig.getReturnUrl());
        String cancelUrl = resolveUrl(request != null ? request.getCancelUrl() : null, payOSConfig.getCancelUrl());

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderCode", orderCode);
        payload.put("amount", amount);
        payload.put("description", description);
        payload.put("cancelUrl", cancelUrl);
        payload.put("returnUrl", returnUrl);
        payload.put("signature", generateSignature(payload));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payOSConfig.getClientId());
            headers.set("x-api-key", payOSConfig.getApiKey());

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(payload, headers);
            RestTemplate restTemplate = new RestTemplate();
            String url = payOSConfig.getBaseUrl() + "/v2/payment-requests";

            log.info("Create Harness payment link: userId={}, plan={}, amount={}, orderCode={}",
                userId, planCode, amount, orderCode);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String code = (String) body.get("code");
                if ("00".equals(code)) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    return HarnessSubscriptionPaymentResponse.builder()
                        .orderCode(orderCode)
                        .amount(amount)
                        .description(description)
                        .paymentLinkId(data != null ? (String) data.get("paymentLinkId") : null)
                        .checkoutUrl(data != null ? (String) data.get("checkoutUrl") : null)
                        .qrCode(data != null ? (String) data.get("qrCode") : null)
                        .status("PENDING")
                        .build();
                }
                String errorDesc = (String) body.get("desc");
                throw new ApiException("Lỗi từ payOS: " + errorDesc, HttpStatus.BAD_REQUEST);
            }

            throw new ApiException("Không thể tạo link thanh toán Harness", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Harness payment link error: {}", e.getMessage(), e);
            throw new ApiException("Không thể tạo link thanh toán Harness: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private int resolveAmount(Integer requestedAmount) {
        if (requestedAmount == null || requestedAmount < 2000) {
            return DEFAULT_MONTHLY_PRICE_VND;
        }
        return requestedAmount;
    }

    private String sanitizePlanCode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return DEFAULT_PLAN_CODE;
        }
        return raw.trim().toLowerCase();
    }

    private String shortPlan(String planCode) {
        String cleaned = planCode.replaceAll("[^a-zA-Z0-9\\-]", "");
        if (cleaned.length() <= 9) {
            return cleaned;
        }
        return cleaned.substring(0, 9);
    }

    private String resolveUrl(String preferred, String fallback) {
        if (preferred != null && !preferred.trim().isEmpty()) {
            return preferred.trim();
        }
        return fallback;
    }

    private String generateSignature(Map<String, Object> payload) {
        String data = "amount=" + payload.get("amount")
            + "&cancelUrl=" + payload.get("cancelUrl")
            + "&description=" + payload.get("description")
            + "&orderCode=" + payload.get("orderCode")
            + "&returnUrl=" + payload.get("returnUrl");
        return hmacSha256(data, payOSConfig.getChecksumKey());
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC signature", e);
        }
    }
}
