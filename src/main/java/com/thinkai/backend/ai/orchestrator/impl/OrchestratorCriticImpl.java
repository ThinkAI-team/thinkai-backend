package com.thinkai.backend.ai.orchestrator.impl;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.orchestrator.OrchestratorCritic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 6: Critic Agent Implementation
 * Review quality c???a response (20% sampling)
 */
@SuppressWarnings("checkstyle:ConstantName")
@Service
public class OrchestratorCriticImpl implements OrchestratorCritic {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorCriticImpl.class);
    private static final double CRITIC_PROBABILITY = 0.2;
    private static final int LONG_RESPONSE_THRESHOLD = 300;

    @Override
    public String review(String response, AgentType agent) {
        if (!shouldReview(response, agent)) {
            return response;
        }

        ReviewResult result = reviewDetailed(response, agent);
        logger.debug("Critic reviewed: score={}, wasModified={}", result.qualityScore(), result.wasModified());

        return result.wasModified() ? result.improvedResponse() : response;
    }

    @Override
    public boolean shouldReview(String response, AgentType agent) {
        // Always review long responses
        if (response.length() > LONG_RESPONSE_THRESHOLD) {
            return true;
        }
        // Random 20% for shorter responses
        return Math.random() < CRITIC_PROBABILITY;
    }

    @Override
    public ReviewResult reviewDetailed(String response, AgentType agent) {
        // Quality scoring based on content analysis

        int qualityScore = 7; // default
        boolean wasModified = false;
        List<String> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // Check for common issues
        if (response.length() < 50) {
            qualityScore -= 2;
            issues.add("Response too brief");
            suggestions.add("Add more detail and examples");
        }

        if (!response.contains("?") && agent == AgentType.CONVERSATION) {
            suggestions.add("Consider asking follow-up questions");
        }

        // TOEIC/IELTS specific
        if ((agent.isToeicAgent() || agent.isIeltsAgent()) &&
            !response.toLowerCase().contains("example")) {
            suggestions.add("Add specific examples");
        }

        return new ReviewResult(
            response, // ch??a modify, ch??? review
            qualityScore,
            issues,
            suggestions,
            wasModified
        );
    }
}
