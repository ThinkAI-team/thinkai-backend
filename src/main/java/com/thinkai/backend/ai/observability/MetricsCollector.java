package com.thinkai.backend.ai.observability;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsCollector {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalTokens = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    private final Map<String, AtomicLong> agentUsage = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> agentErrors = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicLong> statusDistribution = new ConcurrentHashMap<>();

    public void recordRequest(String agent) {
        totalRequests.incrementAndGet();
        agentUsage.computeIfAbsent(agent, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordSuccess(String agent, int tokens, long latencyMs) {
        successfulRequests.incrementAndGet();
        totalTokens.addAndGet(tokens);
        totalLatencyMs.addAndGet(latencyMs);
    }

    public void recordError(String agent, String errorType) {
        failedRequests.incrementAndGet();
        agentErrors.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void recordStatus(int statusCode) {
        statusDistribution.computeIfAbsent(statusCode, k -> new AtomicLong(0)).incrementAndGet();
    }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        long total = totalRequests.get();
        long success = successfulRequests.get();
        long failed = failedRequests.get();

        metrics.put("totalRequests", total);
        metrics.put("successfulRequests", success);
        metrics.put("failedRequests", failed);
        metrics.put("successRate", total > 0 ? (double) success / total * 100 : 0);

        metrics.put("totalTokens", totalTokens.get());
        metrics.put("totalLatencyMs", totalLatencyMs.get());
        metrics.put("avgLatencyMs", total > 0 ? totalLatencyMs.get() / total : 0);

        long cacheTotal = cacheHits.get() + cacheMisses.get();
        double cacheHitRate = cacheTotal > 0 ? (double) cacheHits.get() / cacheTotal * 100 : 0;
        metrics.put("cacheHits", cacheHits.get());
        metrics.put("cacheMisses", cacheMisses.get());
        metrics.put("cacheHitRate", cacheHitRate);

        Map<String, Long> agentUsageMap = new HashMap<>();
        agentUsage.forEach((k, v) -> agentUsageMap.put(k, v.get()));
        metrics.put("agentUsage", agentUsageMap);

        Map<String, Long> errorTypes = new HashMap<>();
        agentErrors.forEach((k, v) -> errorTypes.put(k, v.get()));
        metrics.put("errorTypes", errorTypes);

        Map<Integer, Long> statusDist = new HashMap<>();
        statusDistribution.forEach((k, v) -> statusDist.put(k, v.get()));
        metrics.put("statusDistribution", statusDist);

        return metrics;
    }

    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalTokens.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        totalLatencyMs.set(0);
        agentUsage.clear();
        agentErrors.clear();
        statusDistribution.clear();
    }
}
