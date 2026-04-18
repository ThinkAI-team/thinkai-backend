package com.thinkai.backend.ai.state;

import java.time.Instant;
import java.util.UUID;

/**
 * Record một state transition trong flow
 * Dùng cho observability và debugging
 */
public record AiStateTransition(
    String traceId,
    AiState fromState,
    AiState toState,
    String reason,
    long latencyMs,
    Instant timestamp
) {
    public AiStateTransition {
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static AiStateTransition create(String traceId, AiState fromState, AiState toState, String reason, long latencyMs) {
        return new AiStateTransition(traceId, fromState, toState, reason, latencyMs, Instant.now());
    }

    public static AiStateTransition start(String traceId) {
        return new AiStateTransition(traceId, null, AiState.START, "Request received", 0, Instant.now());
    }
}
