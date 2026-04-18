package com.thinkai.backend.ai.state;

import com.thinkai.backend.ai.config.AgentType;

import java.util.List;
import java.util.Map;

/**
 * Request wrapper cho AI Harness System
 * Tích hợp với AIChatRequest hiện có
 */
public record AiHarnessRequest(
    // Core fields
    String traceId,
    Long userId,
    String conversationId,
    String message,

    // User context
    String userLevel,           // A1, A2, B1, B2, C1, C2
    String targetExam,          // TOEIC, IELTS, or null
    Integer targetScore,        // Target TOEIC/IELTS score

    // Session context
    String lessonContext,       // Current lesson context
    List<String> weakPoints,    // Known weak areas
    List<String> strongPoints,  // Known strong areas

    // Agent hint (optional - for forced routing)
    AgentType preferredAgent,

    // Additional metadata
    Map<String, Object> metadata
) {
    public AiHarnessRequest {
        if (weakPoints == null) {
            weakPoints = List.of();
        }
        if (strongPoints == null) {
            strongPoints = List.of();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    /**
     * Builder pattern for easy creation
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String traceId;
        private Long userId;
        private String conversationId;
        private String message;
        private String userLevel = "B1";
        private String targetExam;
        private Integer targetScore;
        private String lessonContext = "General English";
        private List<String> weakPoints = List.of();
        private List<String> strongPoints = List.of();
        private AgentType preferredAgent;
        private Map<String, Object> metadata = Map.of();

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder userLevel(String userLevel) {
            this.userLevel = userLevel;
            return this;
        }

        public Builder targetExam(String targetExam) {
            this.targetExam = targetExam;
            return this;
        }

        public Builder targetScore(Integer targetScore) {
            this.targetScore = targetScore;
            return this;
        }

        public Builder lessonContext(String lessonContext) {
            this.lessonContext = lessonContext;
            return this;
        }

        public Builder weakPoints(List<String> weakPoints) {
            this.weakPoints = weakPoints != null ? weakPoints : List.of();
            return this;
        }

        public Builder strongPoints(List<String> strongPoints) {
            this.strongPoints = strongPoints != null ? strongPoints : List.of();
            return this;
        }

        public Builder preferredAgent(AgentType preferredAgent) {
            this.preferredAgent = preferredAgent;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : Map.of();
            return this;
        }

        public AiHarnessRequest build() {
            return new AiHarnessRequest(
                traceId, userId, conversationId, message,
                userLevel, targetExam, targetScore, lessonContext,
                weakPoints, strongPoints, preferredAgent, metadata
            );
        }
    }
}
