package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import com.thinkai.backend.ai.state.AiState;
import com.thinkai.backend.ai.state.AiStateTransition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Observability Interface - Phase 9 implement
 * Tracing và monitoring với Langfuse
 */
@Component
public interface OrchestratorObservability {

    /**
     * Bắt đầu trace cho một request
     */
    void startTrace(String traceId, AiHarnessRequest request);

    /**
     * Log state transition
     */
    void logTransition(String traceId, AiStateTransition transition);

    /**
     * Log LLM call
     */
    void logLLMCall(String traceId, String agent, int tokensUsed, long latencyMs);

    /**
     * Log cache hit/miss
     */
    void logCache(String traceId, boolean hit, double similarity);

    /**
     * Log validation result
     */
    void logValidation(String traceId, boolean passed, String notes);

    /**
     * Log critic review
     */
    void logCritic(String traceId, boolean reviewed, int qualityScore);

    /**
     * Kết thúc trace
     */
    void endTrace(String traceId, AiState finalState, List<AiStateTransition> transitions, AiHarnessResponse response);

    /**
     * Get trace metrics
     */
    TraceMetrics getTraceMetrics(String traceId);

    /**
     * Export metrics
     */
    Map<String, Object> exportMetrics();

    record TraceMetrics(
        String traceId,
        long totalLatencyMs,
        int stateCount,
        boolean cacheHit,
        boolean validationPassed,
        boolean criticReviewed,
        String finalAgent,
        AiState finalState
    ) {}
}
