package com.thinkai.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitFilter implements Filter {

    private static final long ONE_MINUTE_MS = 60_000L;
    private static final long CLEANUP_INTERVAL_MS = 15 * ONE_MINUTE_MS;

    private final Map<String, RateLimitCounter> requestCounts = new ConcurrentHashMap<>();
    private volatile long lastCleanupAt = System.currentTimeMillis();

    private static final List<LimitRule> LIMIT_RULES = List.of(
            new LimitRule("/auth/login", 12, ONE_MINUTE_MS),
            new LimitRule("/auth/google", 12, ONE_MINUTE_MS),
            new LimitRule("/auth/forgot-password", 6, ONE_MINUTE_MS),
            new LimitRule("/auth/reset-password", 6, ONE_MINUTE_MS),
            new LimitRule("/auth/register", 8, ONE_MINUTE_MS),
            new LimitRule("/ai-harness", 90, ONE_MINUTE_MS),
            new LimitRule("/ai-tutor", 90, ONE_MINUTE_MS),
            new LimitRule("/notifications/stream", 30, ONE_MINUTE_MS),
            new LimitRule("/api/", 120, ONE_MINUTE_MS)
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);
        String endpoint = httpRequest.getRequestURI();
        cleanupExpiredCountersIfNeeded();
        LimitRule rule = matchRule(endpoint);

        if (rule != null && isRateLimited(clientIp, endpoint, rule)) {
            log.warn("Rate limit hit: ip={}, endpoint={}, rulePrefix={}", clientIp, endpoint, rule.prefix());
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Vượt quá giới hạn request. Vui lòng thử lại sau.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private LimitRule matchRule(String endpoint) {
        for (LimitRule rule : LIMIT_RULES) {
            if (endpoint.startsWith(rule.prefix())) {
                return rule;
            }
        }
        return null;
    }

    private boolean isRateLimited(String clientIp, String endpoint, LimitRule rule) {
        long now = System.currentTimeMillis();
        String key = clientIp + "|" + rule.prefix() + "|" + normalizedPathBucket(endpoint);
        RateLimitCounter counter = requestCounts.computeIfAbsent(key, k -> new RateLimitCounter(now));

        synchronized (counter) {
            if (now - counter.windowStart > rule.windowMs()) {
                counter.windowStart = now;
                counter.count.set(0);
            }

            return counter.count.incrementAndGet() > rule.maxRequests();
        }
    }

    private void cleanupExpiredCountersIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupAt = now;
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > (2 * ONE_MINUTE_MS));
    }

    private String normalizedPathBucket(String endpoint) {
        // Tránh tạo quá nhiều key theo id/path động.
        if (endpoint.startsWith("/api/")) {
            int secondSlash = endpoint.indexOf('/', "/api/".length());
            return secondSlash > 0 ? endpoint.substring(0, secondSlash) : endpoint;
        }
        return endpoint;
    }

    private record LimitRule(String prefix, int maxRequests, long windowMs) {}

    private static class RateLimitCounter {
        long windowStart;
        AtomicInteger count = new AtomicInteger(0);

        RateLimitCounter(long now) {
            this.windowStart = now;
        }
    }
}
