package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.config.AgentType;
import org.springframework.stereotype.Component;

/**
 * Critic Agent Interface - Phase 6 implement
 * Review quality của response (20% sampling)
 */
@Component
public interface OrchestratorCritic {

    /**
     * Review response quality
     *
     * @param response Response cần review
     * @param agent Agent đã tạo response
     * @return Reviewed response (có thể sửa đổi)
     */
    String review(String response, AgentType agent);

    /**
     * Should review this response?
     * 20% sampling hoặc response dài
     */
    boolean shouldReview(String response, AgentType agent);

    /**
     * Detailed review với feedback
     */
    ReviewResult reviewDetailed(String response, AgentType agent);

    record ReviewResult(
        String improvedResponse,
        int qualityScore,       // 1-10
        java.util.List<String> issues,
        java.util.List<String> suggestions,
        boolean wasModified
    ) {}
}
