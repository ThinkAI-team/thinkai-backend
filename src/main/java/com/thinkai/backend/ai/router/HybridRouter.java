package com.thinkai.backend.ai.router;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid Router - kết hợp Keyword + Embedding routing
 * Sử dụng weighted scoring để chọn agent phù hợp nhất
 */
@Component
@SuppressWarnings("checkstyle:ConstantName")
public class HybridRouter {

    private static final Logger logger = LoggerFactory.getLogger(HybridRouter.class);

    private final KeywordRouter keywordRouter;
    private final EmbeddingRouter embeddingRouter;

    // Weights cho hybrid scoring
    private static final double KEYWORD_WEIGHT = 0.6;
    private static final double EMBEDDING_WEIGHT = 0.4;

    // Thresholds
    private static final double CONFIDENCE_HIGH = 0.8;
    private static final double CONFIDENCE_MEDIUM = 0.5;
    private static final double CONFIDENCE_LOW = 0.3;

    public HybridRouter(KeywordRouter keywordRouter, EmbeddingRouter embeddingRouter) {
        this.keywordRouter = keywordRouter;
        this.embeddingRouter = embeddingRouter;
    }

    /**
     * Route request sử dụng hybrid approach
     *
     * @param request User request
     * @return Routing result với agent và confidence score
     */
    public RoutingResult route(AiHarnessRequest request) {
        String message = request.message();
        if (message == null || message.isBlank()) {
            return RoutingResult.fallback(AgentType.CONVERSATION, "empty_request");
        }

        // Get scores từ cả 2 routers
        List<KeywordRouter.RoutingScore> keywordScores = keywordRouter.route(request);
        List<EmbeddingRouter.RoutingScore> embeddingScores = embeddingRouter.route(request);

        // Combine scores
        Map<AgentType, CombinedScore> combinedScores = combineScores(keywordScores, embeddingScores);

        // Sort và lấy top
        List<CombinedScore> sorted = new ArrayList<>(combinedScores.values());
        sorted.sort((a, b) -> Double.compare(b.totalScore(), a.totalScore()));

        if (sorted.isEmpty()) {
            logger.debug("No confident match for '{}', falling back to CONVERSATION",
                message.substring(0, Math.min(50, message.length())));
            return RoutingResult.fallback(AgentType.CONVERSATION, "no_match");
        }

        CombinedScore top = sorted.get(0);
        double confidence = calculateConfidence(top, sorted.size() > 1 ? sorted.get(1) : null);

        // Log routing decision
        logger.info("[ROUTER] Input: '{}' -> Agent: {}, Confidence: {:.3f} (keyword={:.3f}, embedding={:.3f})",
            message.substring(0, Math.min(60, message.length())),
            top.agent(),
            confidence,
            top.keywordScore(),
            top.embeddingScore());

        return new RoutingResult(
            top.agent(),
            confidence,
            buildRoutingReason(top),
            top.keywordScore() > 0,
            top.embeddingScore() > 0
        );
    }

    /**
     * Route với forced agent (nếu user chỉ định)
     */
    public RoutingResult routeWithPreference(AiHarnessRequest request, AgentType preferredAgent) {
        if (preferredAgent != null) {
            logger.debug("Using preferred agent: {}", preferredAgent);
            return new RoutingResult(
                preferredAgent,
                1.0,
                "user_preference",
                false,
                false
            );
        }
        return route(request);
    }

    /**
     * Combine keyword và embedding scores
     */
    private Map<AgentType, CombinedScore> combineScores(
            List<KeywordRouter.RoutingScore> keywordScores,
            List<EmbeddingRouter.RoutingScore> embeddingScores) {

        Map<AgentType, CombinedScore> result = new EnumMap<>(AgentType.class);

        // Normalize keyword scores (0-1)
        double maxKeywordScore = keywordScores.stream()
            .mapToDouble(KeywordRouter.RoutingScore::score)
            .max()
            .orElse(1.0);

        // Add keyword scores
        for (KeywordRouter.RoutingScore score : keywordScores) {
            double normalizedScore = maxKeywordScore > 0 ? score.score() / maxKeywordScore : 0;
            result.put(score.agent(), new CombinedScore(
                score.agent(),
                normalizedScore * KEYWORD_WEIGHT,
                normalizedScore,
                0.0,
                "keyword"
            ));
        }

        // Add embedding scores
        for (EmbeddingRouter.RoutingScore score : embeddingScores) {
            CombinedScore existing = result.get(score.agent());
            double embeddingScore = score.score() * EMBEDDING_WEIGHT;

            if (existing != null) {
                // Update existing
                result.put(score.agent(), new CombinedScore(
                    score.agent(),
                    existing.keywordScore() + embeddingScore,
                    existing.keywordScore(),
                    score.score(),
                    existing.keywordScore() > 0 ? "hybrid" : "embedding"
                ));
            } else {
                result.put(score.agent(), new CombinedScore(
                    score.agent(),
                    embeddingScore,
                    0.0,
                    score.score(),
                    "embedding"
                ));
            }
        }

        return result;
    }

    /**
     * Calculate confidence score
     */
    private double calculateConfidence(CombinedScore top, CombinedScore second) {
        double baseConfidence = top.totalScore();

        // Boost nếu cả keyword và embedding đều match
        if (top.keywordScore() > 0 && top.embeddingScore() > 0) {
            baseConfidence *= 1.2;
        }

        // Boost nếu gap với second place lớn
        if (second != null && second.totalScore() > 0) {
            double gap = top.totalScore() - second.totalScore();
            baseConfidence += gap * 0.5;
        }

        return Math.min(1.0, baseConfidence);
    }

    /**
     * Build routing reason string
     */
    private String buildRoutingReason(CombinedScore score) {
        StringBuilder reason = new StringBuilder();

        if (score.keywordScore() > 0) {
            reason.append("keyword_match");
        }
        if (score.embeddingScore() > 0) {
            if (reason.length() > 0) {
                reason.append("+");
            }
            reason.append("embedding_match");
        }

        return reason.toString();
    }

    /**
     * Get routing explanation cho debugging
     */
    public RoutingExplanation explain(AiHarnessRequest request) {
        List<KeywordRouter.RoutingScore> keywordScores = keywordRouter.route(request);
        List<EmbeddingRouter.RoutingScore> embeddingScores = embeddingRouter.route(request);
        RoutingResult finalResult = route(request);

        return new RoutingExplanation(
            request.message(),
            keywordScores,
            embeddingScores,
            finalResult
        );
    }

    // Record classes

    private record CombinedScore(
        AgentType agent,
        double totalScore,
        double keywordScore,
        double embeddingScore,
        String matchType
    ) {}

    public record RoutingResult(
        AgentType agent,
        double confidence,
        String reason,
        boolean keywordMatched,
        boolean embeddingMatched
    ) {
        public boolean isConfident() {
            return confidence >= CONFIDENCE_MEDIUM;
        }

        public boolean isHighConfidence() {
            return confidence >= CONFIDENCE_HIGH;
        }

        public String confidenceLevel() {
            if (confidence >= CONFIDENCE_HIGH) {
                return "HIGH";
            }
            if (confidence >= CONFIDENCE_MEDIUM) {
                return "MEDIUM";
            }
            if (confidence >= CONFIDENCE_LOW) {
                return "LOW";
            }
            return "VERY_LOW";
        }

        public static RoutingResult fallback(AgentType agent, String reason) {
            return new RoutingResult(agent, 0.0, reason, false, false);
        }
    }

    public record RoutingExplanation(
        String input,
        List<KeywordRouter.RoutingScore> keywordScores,
        List<EmbeddingRouter.RoutingScore> embeddingScores,
        RoutingResult finalResult
    ) {}
}
