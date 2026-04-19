package com.thinkai.backend.service;

import com.thinkai.backend.config.PayOSConfig;
import com.thinkai.backend.dto.PaymentRequest;
import com.thinkai.backend.dto.PaymentResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Payment;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.PaymentRepository;
import com.thinkai.backend.repository.UserRepository;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private static final BigDecimal DIRECT_ENROLL_PRICE_THRESHOLD = new BigDecimal("10000");


    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final PayOSConfig payOSConfig;

    @Transactional
    public PaymentResponse createPaymentLink(Long userId, PaymentRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ApiException("Không tìm thấy khóa học", HttpStatus.NOT_FOUND));

        if (!course.getIsPublished()) {
            throw new ApiException("Khóa học chưa được xuất bản", HttpStatus.BAD_REQUEST);
        }
        if (course.getStatus() == Course.Status.BLOCKED) {
            throw new ApiException("Khóa học đã bị khóa bởi quản trị viên", HttpStatus.BAD_REQUEST);
        }
        if (course.getStatus() != Course.Status.APPROVED) {
            throw new ApiException("Khóa học chưa sẵn sàng để thanh toán", HttpStatus.BAD_REQUEST);
        }

        if (paymentRepository.existsByUserIdAndCourseIdAndStatus(userId, request.getCourseId(), Payment.PaymentStatus.COMPLETED)) {
            throw new ApiException("Bạn đã mua khóa học này rồi", HttpStatus.BAD_REQUEST);
        }

        Long orderCode = System.currentTimeMillis();

        String description = "ThinkAI";
        if (course.getTitle() != null && !course.getTitle().isEmpty()) {
            description = course.getTitle().length() > 9 
                ? course.getTitle().substring(0, 9) 
                : course.getTitle();
        }

        BigDecimal coursePrice = course.getPrice() == null ? BigDecimal.ZERO : course.getPrice();
        int amount = coursePrice.intValue();

        if (coursePrice.compareTo(DIRECT_ENROLL_PRICE_THRESHOLD) < 0) {
            return createDirectEnrollPayment(userId, course, orderCode, amount, description);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderCode", orderCode);
        payload.put("amount", amount);
        payload.put("description", description);
        payload.put("cancelUrl", payOSConfig.getCancelUrl());
        payload.put("returnUrl", payOSConfig.getReturnUrl());

        String signature = generateSignature(payload);
        payload.put("signature", signature);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", payOSConfig.getClientId());
            headers.set("x-api-key", payOSConfig.getApiKey());

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(payload, headers);
            RestTemplate restTemplate = new RestTemplate();

            String url = payOSConfig.getBaseUrl() + "/v2/payment-requests";
            log.info("Calling payOS API: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                String code = (String) responseBody.get("code");
                if ("00".equals(code)) {
                    Payment payment = Payment.builder()
                            .orderCode(orderCode)
                            .userId(userId)
                            .courseId(course.getId())
                            .amount(amount)
                            .status(Payment.PaymentStatus.PENDING)
                            .paymentLinkId((String) data.get("paymentLinkId"))
                            .checkoutUrl((String) data.get("checkoutUrl"))
                            .qrCode((String) data.get("qrCode"))
                            .description(description)
                            .build();

                    payment = paymentRepository.save(payment);
                    log.info("Payment created: orderCode={}", orderCode);
                    return PaymentResponse.fromEntity(payment);
                } else {
                    String errorDesc = (String) responseBody.get("desc");
                    log.error("payOS error: code={}, desc={}", code, errorDesc);
                    throw new ApiException("Lỗi từ payOS: " + errorDesc, HttpStatus.BAD_REQUEST);
                }
            }

            throw new ApiException("Lỗi khi tạo link thanh toán", HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating payment link: {}", e.getMessage(), e);
            throw new ApiException("Lỗi khi tạo link thanh toán: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private PaymentResponse createDirectEnrollPayment(
            Long userId,
            Course course,
            Long orderCode,
            int amount,
            String description) {
        Payment payment = Payment.builder()
                .orderCode(orderCode)
                .userId(userId)
                .courseId(course.getId())
                .amount(amount)
                .status(Payment.PaymentStatus.COMPLETED)
                .description(description)
                .completedAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);

        try {
            courseService.enrollCourse(course.getId(), userId);
            log.info("Direct enrolled (price<10000): userId={}, courseId={}", userId, course.getId());
        } catch (Exception e) {
            log.warn("Direct enroll skipped (maybe already enrolled): {}", e.getMessage());
        }

        return PaymentResponse.fromEntity(payment);
    }

    public PaymentResponse getPaymentByOrderCode(Long orderCode) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException("Không tìm thấy thanh toán", HttpStatus.NOT_FOUND));

        PaymentResponse response = PaymentResponse.fromEntity(payment);

        courseRepository.findById(payment.getCourseId()).ifPresent(course -> {
            response.setCourseTitle(course.getTitle());
        });

        return response;
    }

    /**
     * Confirm payment - gọi sau khi PayOS redirect về với status=PAID
     * Frontend gọi endpoint này để xác nhận thanh toán và enroll user
     */
    @Transactional
    public PaymentResponse confirmPayment(Long orderCode, Long userId) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException("Không tìm thấy thanh toán", HttpStatus.NOT_FOUND));

        // Kiểm tra payment thuộc về user
        if (!payment.getUserId().equals(userId)) {
            throw new ApiException("Thanh toán không thuộc về bạn", HttpStatus.FORBIDDEN);
        }

        // Nếu đã COMPLETED rồi thì trả về luôn
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            PaymentResponse response = PaymentResponse.fromEntity(payment);
            courseRepository.findById(payment.getCourseId()).ifPresent(course -> {
                response.setCourseTitle(course.getTitle());
            });
            return response;
        }

        // Gọi PayOS API để check trạng thái thật
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", payOSConfig.getClientId());
            headers.set("x-api-key", payOSConfig.getApiKey());

            HttpEntity<Void> httpRequest = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            String url = payOSConfig.getBaseUrl() + "/v2/payment-requests/" + orderCode;
            log.info("Checking payment status from payOS: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String code = (String) responseBody.get("code");

                if ("00".equals(code)) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    String payosStatus = (String) data.get("status");

                    log.info("PayOS status for orderCode={}: {}", orderCode, payosStatus);

                    if ("PAID".equals(payosStatus)) {
                        payment.setStatus(Payment.PaymentStatus.COMPLETED);
                        payment.setCompletedAt(LocalDateTime.now());
                        paymentRepository.save(payment);

                        // Enroll user vào course
                        try {
                            courseService.enrollCourse(payment.getCourseId(), payment.getUserId());
                            log.info("User enrolled: userId={}, courseId={}", payment.getUserId(), payment.getCourseId());
                        } catch (Exception e) {
                            log.warn("Enroll error (maybe already enrolled): {}", e.getMessage());
                        }
                    } else if ("CANCELLED".equals(payosStatus) || "EXPIRED".equals(payosStatus)) {
                        payment.setStatus(Payment.PaymentStatus.CANCELLED);
                        paymentRepository.save(payment);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking payOS status: {}", e.getMessage(), e);
        }

        PaymentResponse result = PaymentResponse.fromEntity(payment);
        courseRepository.findById(payment.getCourseId()).ifPresent(course -> {
            result.setCourseTitle(course.getTitle());
        });
        return result;
    }

    private String generateSignature(Map<String, Object> payload) {
        String data = "amount=" + payload.get("amount") +
                "&cancelUrl=" + payload.get("cancelUrl") +
                "&description=" + payload.get("description") +
                "&orderCode=" + payload.get("orderCode") +
                "&returnUrl=" + payload.get("returnUrl");
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
