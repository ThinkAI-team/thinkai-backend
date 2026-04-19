package com.thinkai.backend.ai.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("checkstyle:ConstantName")
@Component
public class TraceLogger {

    private static final Logger log = LoggerFactory.getLogger(TraceLogger.class);

    private final Map<String, TraceData> activeTraces = new ConcurrentHashMap<>();
    private final List<TraceData> completedTraces = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_COMPLETED_TRACES = 1000;

    public void startTrace(String traceId, String userId, String message) {
        TraceData data = new TraceData(
            traceId,
            userId,
            message,
            Instant.now().toEpochMilli(),
            new ArrayList<>(),
            new HashMap<>()
        );
        activeTraces.put(traceId, data);
        log.info("[TRACE] Started: traceId={}, userId={}, message={}",
            traceId, userId, message != null ? message.substring(0, Math.min(50, message.length())) : "");
    }

    public void logTransition(String traceId, String fromState, String toState, String action) {
        TraceData data = activeTraces.get(traceId);
        if (data != null) {
            data.transitions().add(new TransitionRecord(fromState, toState, action, Instant.now().toEpochMilli()));
            log.debug("[TRACE] {} -> {} ({}): traceId={}", fromState, toState, action, traceId);
        }
    }

    public void logLLMCall(String traceId, String agent, int tokens, long latency) {
        TraceData data = activeTraces.get(traceId);
        if (data != null) {
            data.metrics().put("llm_agent", agent);
            data.metrics().put("llm_tokens", tokens);
            data.metrics().put("llm_latency_ms", latency);
            log.info("[TRACE] LLM call: traceId={}, agent={}, tokens={}, latency={}ms",
                traceId, agent, tokens, latency);
        }
    }

    public void logCache(String traceId, boolean hit, double similarity) {
        TraceData data = activeTraces.get(traceId);
        if (data != null) {
            data.metrics().put("cache_hit", hit);
            data.metrics().put("cache_similarity", similarity);
            log.info("[TRACE] Cache: traceId={}, hit={}, similarity={}", traceId, hit, similarity);
        }
    }

    public void logValidation(String traceId, boolean passed, String notes) {
        TraceData data = activeTraces.get(traceId);
        if (data != null) {
            data.metrics().put("validation_passed", passed);
            data.metrics().put("validation_notes", notes);
            log.info("[TRACE] Validation: traceId={}, passed={}, notes={}", traceId, passed, notes);
        }
    }

    public void logCritic(String traceId, boolean reviewed, int qualityScore) {
        TraceData data = activeTraces.get(traceId);
        if (data != null) {
            data.metrics().put("critic_reviewed", reviewed);
            data.metrics().put("critic_quality_score", qualityScore);
            log.info("[TRACE] Critic: traceId={}, reviewed={}, score={}", traceId, reviewed, qualityScore);
        }
    }

    public void logTool(String traceId, String toolName, boolean success, long duration) {
        TraceData data = activeTraces.get(traceId);
        if (data != null) {
            log.info("[TRACE] Tool: traceId={}, tool={}, success={}, duration={}ms",
                traceId, toolName, success, duration);
        }
    }

    public void logError(String traceId, String error) {
        TraceData data = activeTraces.get(traceId);
        if (data != null) {
            data.metrics().put("error", error);
            log.error("[TRACE] Error: traceId={}, error={}", traceId, error);
        }
    }

    public void endTrace(String traceId, String finalState, String finalAgent, long totalLatency) {
        TraceData data = activeTraces.remove(traceId);
        if (data != null) {
            data.setFinalState(finalState);
            data.setFinalAgent(finalAgent);
            data.setEndTime(Instant.now().toEpochMilli());
            data.setTotalLatencyMs(totalLatency);

            completedTraces.add(data);
            if (completedTraces.size() > MAX_COMPLETED_TRACES) {
                completedTraces.remove(0);
            }

            log.info("[TRACE] Ended: traceId={}, state={}, agent={}, latency={}ms",
                traceId, finalState, finalAgent, totalLatency);
        }
    }

    public Optional<TraceData> getTrace(String traceId) {
        TraceData active = activeTraces.get(traceId);
        if (active != null) {
            return Optional.of(active);
        }
        return completedTraces.stream()
            .filter(t -> t.traceId().equals(traceId))
            .findFirst();
    }

    public List<TraceData> getRecentTraces(int count) {
        List<TraceData> recent = new ArrayList<>(completedTraces);
        Collections.reverse(recent);
        return recent.subList(0, Math.min(count, recent.size()));
    }

    public static class TraceData {
        private final String traceId;
        private final String userId;
        private final String message;
        private final long startTime;
        private final List<TransitionRecord> transitions;
        private final Map<String, Object> metrics;
        private String finalState;
        private String finalAgent;
        private long endTime;
        private long totalLatencyMs;

        public TraceData(String traceId, String userId, String message, long startTime,
                        List<TransitionRecord> transitions, Map<String, Object> metrics) {
            this.traceId = traceId;
            this.userId = userId;
            this.message = message;
            this.startTime = startTime;
            this.transitions = transitions;
            this.metrics = metrics;
        }

        public String traceId() { return traceId; }
        public String userId() { return userId; }
        public String message() { return message; }
        public long startTime() { return startTime; }
        public List<TransitionRecord> transitions() { return transitions; }
        public Map<String, Object> metrics() { return metrics; }
        public String finalState() { return finalState; }
        public String finalAgent() { return finalAgent; }
        public long endTime() { return endTime; }
        public long totalLatencyMs() { return totalLatencyMs; }

        public void setFinalState(String finalState) { this.finalState = finalState; }
        public void setFinalAgent(String finalAgent) { this.finalAgent = finalAgent; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public void setTotalLatencyMs(long totalLatencyMs) { this.totalLatencyMs = totalLatencyMs; }
    }

    public record TransitionRecord(
        String fromState,
        String toState,
        String action,
        long timestamp
    ) {}
}
