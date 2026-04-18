package com.thinkai.backend.ai.critic;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.llm.LLMService;
import com.thinkai.backend.ai.validator.SafetyChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CriticService {

    private static final Logger log = LoggerFactory.getLogger(CriticService.class);
    private static final double CRITIC_PROBABILITY = 0.2;
    private static final int LONG_RESPONSE_THRESHOLD = 300;
    private static final int MIN_QUALITY_THRESHOLD = 60;

    private final QualityScorer qualityScorer;
    private final SafetyChecker safetyChecker;

    public CriticService(QualityScorer qualityScorer, SafetyChecker safetyChecker) {
        this.qualityScorer = qualityScorer;
        this.safetyChecker = safetyChecker;
    }

    public boolean shouldReview(String response, AgentType agent) {
        if (response == null || response.isBlank()) {
            return false;
        }
        
        if (response.length() > LONG_RESPONSE_THRESHOLD) {
            return true;
        }
        
        return Math.random() < CRITIC_PROBABILITY;
    }

    public CriticResult review(String response, AgentType agent) {
        if (response == null || response.isBlank()) {
            return new CriticResult(
                "Response is empty",
                0,
                List.of("Empty response"),
                List.of("Provide a valid response"),
                true,
                false
            );
        }

        var safetyResult = safetyChecker.check(response);
        if (!safetyResult.safe()) {
            return new CriticResult(
                "Content blocked for safety",
                0,
                safetyResult.issues(),
                List.of("Review content for safety violations"),
                true,
                false
            );
        }

        var qualityScore = qualityScorer.score(response, agent);
        boolean needsImprovement = qualityScore.totalScore() < MIN_QUALITY_THRESHOLD;
        
        List<String> suggestions = new ArrayList<>();
        if (needsImprovement) {
            suggestions.addAll(qualityScore.failures());
            
            if (response.length() < 50) {
                suggestions.add("Add more detail and examples");
            }
            
            if (agent.isToeicAgent() || agent.isIeltsAgent()) {
                if (!response.toLowerCase().contains("example")) {
                    suggestions.add("Add specific examples for better understanding");
                }
                if (!response.toLowerCase().contains("explanation")) {
                    suggestions.add("Include explanation for answers");
                }
            }
            
            if (agent == AgentType.CONVERSATION) {
                if (!response.contains("?")) {
                    suggestions.add("Consider asking follow-up questions");
                }
            }
        }

        return new CriticResult(
            response,
            qualityScore.totalScore(),
            qualityScore.failures(),
            suggestions,
            needsImprovement,
            false
        );
    }

    public record CriticResult(
        String response,
        int qualityScore,
        List<String> issues,
        List<String> suggestions,
        boolean needsImprovement,
        boolean wasModified
    ) {}
}
