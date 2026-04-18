package com.thinkai.backend.ai.orchestrator.impl;

import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import com.thinkai.backend.ai.state.AiState;
import com.thinkai.backend.ai.state.AiStateTransition;
import com.thinkai.backend.ai.orchestrator.OrchestratorObservability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 9: Observability Implementation
 * Tracing với Langfuse
 * Tạm thời dùng SLF4J logging
 */
@Service
public class OrchestratorObservabilityImpl implements OrchestratorObservability {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorObservabilityImpl.class);

    // In-memory trace storage (sẽ thay bằng Langfuse ở Phase 9)
    private final Map<String, TraceData> traces = new ConcurrentHashMap<>();

    @Override
    public void startTrace(String traceId, AiHarnessRequest request) {
        traces.put(traceId, new TraceData(traceId, System.currentTimeMillis(), request));
        logger.info("[{}] Trace started for user={}", traceId, request.userId());
    }

    @Override
    public void logTransition(String traceId, AiStateTransition transition) {
        TraceData trace = traces.get(traceId);
        if (trace != null) {
            trace.addTransition(transition);
        }
        logger.debug("[{}] State: {} -> {}", traceId, transition.fromState(), transition.toState());
    }

    @Override
    public void logLLMCall(String traceId, String agent, int tokensUsed, long latencyMs) {
        logger.debug("[{}] LLM call: agent={}, tokens={}, latency={}ms", traceId, agent, tokensUsed, latencyMs);
    }

    @Override
    public void logCache(String traceId, boolean hit, double similarity) {
        logger.debug("[{}] Cache: hit={}, similarity={}", traceId, hit, similarity);
    }

    @Override
    public void logValidation(String traceId, boolean passed, String notes) {
        logger.debug("[{}] Validation: passed={}, notes={}", traceId, passed, notes);
    }

    @Override
    public void logCritic(String traceId, boolean reviewed, int qualityScore) {
        logger.debug("[{}] Critic: reviewed={}, score={}", traceId, reviewed, qualityScore);
    }

    @Override
    public void endTrace(String traceId, AiState finalState, List<AiStateTransition> transitions, AiHarnessResponse response) {
        TraceData trace = traces.get(traceId);
        if (trace != null) {
            long duration = System.currentTimeMillis() - trace.startTime;
            logger.info("[{}] Trace completed: finalState={}, duration={}ms, agent={}",
                traceId, finalState, duration, response.agentType());
        }
    }

    @Override
    public TraceMetrics getTraceMetrics(String traceId) {
        TraceData trace = traces.get(traceId);
        if (trace == null) {
            return null;
        }
        return new TraceMetrics(
            traceId,
            System.currentTimeMillis() - trace.startTime,
            trace.transitions.size(),
            false, // cache hit
            true,  // validation passed
            false, // critic reviewed
            null,  // final agent
            trace.transitions.isEmpty() ? AiState.START : trace.transitions.get(trace.transitions.size() - 1).toState()
        );
    }

    @Override
    public Map<String, Object> exportMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("totalTraces", traces.size());
        return metrics;
    }

    // Internal trace data class
    private static class TraceData {
        final String traceId;
        final long startTime;
        final AiHarnessRequest request;
        final List<AiStateTransition> transitions = new java.util.ArrayList<>();

        TraceData(String traceId, long startTime, AiHarnessRequest request) {
            this.traceId = traceId;
            this.startTime = startTime;
            this.request = request;
        }

        void addTransition(AiStateTransition transition) {
            transitions.add(transition);
        }
    }
}
