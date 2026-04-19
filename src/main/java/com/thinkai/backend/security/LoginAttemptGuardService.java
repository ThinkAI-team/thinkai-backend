package com.thinkai.backend.security;

import com.thinkai.backend.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptGuardService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long WINDOW_MS = 10 * 60_000L;
    private static final long BLOCK_MS = 15 * 60_000L;
    private static final long CLEANUP_INTERVAL_MS = 10 * 60_000L;

    private final Map<String, AttemptState> states = new ConcurrentHashMap<>();
    private volatile long lastCleanupAt = System.currentTimeMillis();

    public void assertNotBlocked(String email, String clientIp) {
        cleanupIfNeeded();
        AttemptState state = states.get(buildKey(email, clientIp));
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.blockedUntil > now) {
                throw new ApiException(
                        "Bạn đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau 15 phút.",
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
            if (now - state.firstFailedAt > WINDOW_MS) {
                states.remove(buildKey(email, clientIp));
            }
        }
    }

    public void recordFailure(String email, String clientIp) {
        cleanupIfNeeded();
        long now = System.currentTimeMillis();
        String key = buildKey(email, clientIp);
        AttemptState state = states.computeIfAbsent(key, k -> new AttemptState(now));

        synchronized (state) {
            if (now - state.firstFailedAt > WINDOW_MS) {
                state.firstFailedAt = now;
                state.failedCount = 0;
                state.blockedUntil = 0L;
            }
            state.failedCount++;
            if (state.failedCount >= MAX_FAILED_ATTEMPTS) {
                state.blockedUntil = now + BLOCK_MS;
            }
        }
    }

    public void recordSuccess(String email, String clientIp) {
        states.remove(buildKey(email, clientIp));
    }

    public String extractClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildKey(String email, String clientIp) {
        return normalizeEmail(email) + "|" + (clientIp == null ? "unknown" : clientIp);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupAt = now;
        states.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            long staleAt = Math.max(state.firstFailedAt + WINDOW_MS, state.blockedUntil + WINDOW_MS);
            return now > staleAt;
        });
    }

    private static final class AttemptState {
        long firstFailedAt;
        int failedCount;
        long blockedUntil;

        AttemptState(long now) {
            this.firstFailedAt = now;
            this.failedCount = 0;
            this.blockedUntil = 0L;
        }
    }
}

