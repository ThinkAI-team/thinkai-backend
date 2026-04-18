package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Semantic Cache Interface - Phase 4 implement
 * Tích hợp với Redis cho semantic similarity search
 */
@Component
public interface OrchestratorCache {

    /**
     * Check cache với semantic similarity
     *
     * @param request User request
     * @param agent Selected agent
     * @return Cached response nếu tìm thấy (similarity > threshold)
     */
    Optional<CachedEntry> check(AiHarnessRequest request, AgentType agent);

    /**
     * Save response vào cache
     */
    void save(AiHarnessRequest request, AiHarnessResponse response, AgentType agent);

    /**
     * Invalidate cache cho user/agent
     */
    void invalidate(Long userId, AgentType agent);

    /**
     * Cache entry
     */
    record CachedEntry(
        String response,
        double similarity,
        long timestamp,
        String originalQuery
    ) {}
}
