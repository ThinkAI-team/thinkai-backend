package com.thinkai.backend.ai.router;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Keyword-based Router
 * Định tuyến dựa trên keyword matching với TF-IDF scoring
 */
@Component
public class KeywordRouter {

    private static final Logger logger = LoggerFactory.getLogger(KeywordRouter.class);

    private final AgentRegistry agentRegistry;

    // Boost factors cho các keyword types
    private static final double EXACT_MATCH_BOOST = 2.0;
    private static final double PARTIAL_MATCH_BOOST = 1.0;
    private static final double EXAM_SPECIFIC_BOOST = 1.5;

    public KeywordRouter(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    /**
     * Route request using keyword matching
     *
     * @param request User request
     * @return Scored routing results (sorted by score descending)
     */
    public List<RoutingScore> route(AiHarnessRequest request) {
        String message = request.message().toLowerCase();
        String normalized = normalize(message);

        List<RoutingScore> scores = new ArrayList<>();

        for (AgentType agent : agentRegistry.getAllAgents()) {
            Optional<AgentRegistry.AgentMetadata> metadataOpt = agentRegistry.get(agent);
            if (metadataOpt.isEmpty()) continue;

            AgentRegistry.AgentMetadata metadata = metadataOpt.get();
            double score = calculateScore(normalized, metadata);

            if (score > 0) {
                scores.add(new RoutingScore(agent, score, "keyword_match"));
            }
        }

        // Sort by score descending
        scores.sort((a, b) -> Double.compare(b.score(), a.score()));

        logger.debug("Keyword routing for '{}': top agent={}, score={}",
            request.message().substring(0, Math.min(50, request.message().length())),
            scores.isEmpty() ? "none" : scores.get(0).agent(),
            scores.isEmpty() ? 0 : scores.get(0).score());

        return scores;
    }

    /**
     * Calculate keyword match score
     */
    private double calculateScore(String message, AgentRegistry.AgentMetadata metadata) {
        double totalScore = 0.0;
        int matchCount = 0;

        for (String keyword : metadata.keywords()) {
            String normalizedKeyword = keyword.toLowerCase();

            // Exact match (phrases)
            if (message.contains(normalizedKeyword)) {
                double boost = EXACT_MATCH_BOOST;

                // Extra boost for exam-specific keywords (toeic/ielts)
                if (normalizedKeyword.contains("toeic") || normalizedKeyword.contains("ielts")) {
                    boost *= EXAM_SPECIFIC_BOOST;
                }

                totalScore += boost;
                matchCount++;
                continue;
            }

            // Partial match (word by word)
            String[] keywordParts = normalizedKeyword.split("\\s+");
            if (keywordParts.length > 1) {
                int partialMatches = 0;
                for (String part : keywordParts) {
                    if (part.length() > 2 && message.contains(part)) {
                        partialMatches++;
                    }
                }

                if (partialMatches == keywordParts.length) {
                    totalScore += EXACT_MATCH_BOOST * 0.8; // Partial exact match
                    matchCount++;
                } else if (partialMatches > 0) {
                    totalScore += PARTIAL_MATCH_BOOST * (partialMatches / (double) keywordParts.length);
                    matchCount++;
                }
            }
        }

        // Normalize by number of keywords
        if (metadata.keywords().isEmpty()) return 0.0;

        // Score = (total score / keyword count) * sqrt(match count)
        double normalizedScore = (totalScore / metadata.keywords().size()) * Math.sqrt(matchCount);

        // Apply agent routing weight
        return normalizedScore * metadata.routingWeight();
    }

    /**
     * Normalize message for matching
     */
    private String normalize(String message) {
        return message
            .toLowerCase()
            .replaceAll("[\\p{Punct}]", " ")  // Remove punctuation
            .replaceAll("\\s+", " ")           // Normalize whitespace
            .trim();
    }

    /**
     * Get top agent from keyword routing
     */
    public Optional<RoutingScore> getTopAgent(AiHarnessRequest request) {
        List<RoutingScore> scores = route(request);
        return scores.isEmpty() ? Optional.empty() : Optional.of(scores.get(0));
    }

    /**
     * Routing score record
     */
    public record RoutingScore(
        AgentType agent,
        double score,
        String reason
    ) {}
}
