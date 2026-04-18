package com.thinkai.backend.ai.state;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.dto.AiTutorUiAction;

import java.util.List;
import java.util.Map;

/**
 * Response wrapper cho AI Harness System
 * Tích hợp với AIChatResponse hiện có
 */
public record AiHarnessResponse(
    // Core fields
    String content,
    String conversationId,
    Long messageId,

    // Agent info
    AgentType agentType,
    String agentName,

    // State info
    AiState finalState,
    List<AiStateTransition> stateTransitions,

    // Quality metrics
    boolean validated,
    boolean criticReviewed,
    String validationNotes,

    // UI actions
    List<AiTutorUiAction> uiActions,

    // Performance
    int responseTimeMs,
    int tokensUsed,
    boolean cacheHit,

    // Context for next turn
    Map<String, Object> contextHints,

    // Thinking process
    List<ThinkingStep> thinkingSteps,

    // Error info (if any)
    String errorCode,
    String errorMessage
) {
    public AiHarnessResponse {
        if (uiActions == null) {
            uiActions = List.of();
        }
        if (stateTransitions == null) {
            stateTransitions = List.of();
        }
        if (contextHints == null) {
            contextHints = Map.of();
        }
        if (thinkingSteps == null) {
            thinkingSteps = List.of();
        }
    }

    /**
     * Helper method to create new instance with updated thinkingSteps
     */
    public AiHarnessResponse withThinkingSteps(List<ThinkingStep> steps) {
        return new AiHarnessResponse(
            content, conversationId, messageId,
            agentType, agentName, finalState, stateTransitions,
            validated, criticReviewed, validationNotes,
            uiActions, responseTimeMs, tokensUsed, cacheHit,
            contextHints, steps, errorCode, errorMessage
        );
    }

    /**
     * Thinking step record - chi tiet từng step trong flow
     */
    public record ThinkingStep(
        String step,           // Tên step: "Routing", "Cache", "Context", "LLM", "Validation", "Critic"
        String description,    // Mô tả chi tiết: "Agent: GRAMMAR, confidence: 0.85"
        long latencyMs,       // Thời gian thực hiện
        boolean success       // Thành công hay thất bại
    ) {}

    /**
     * Builder pattern
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String content;
        private String conversationId;
        private Long messageId;
        private AgentType agentType;
        private String agentName;
        private AiState finalState = AiState.DONE;
        private List<AiStateTransition> stateTransitions = List.of();
        private boolean validated = true;
        private boolean criticReviewed = false;
        private String validationNotes;
        private List<AiTutorUiAction> uiActions = List.of();
        private int responseTimeMs;
        private int tokensUsed;
        private boolean cacheHit = false;
        private Map<String, Object> contextHints = Map.of();
        private List<ThinkingStep> thinkingSteps = List.of();
        private String errorCode;
        private String errorMessage;

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder messageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder agentType(AgentType agentType) {
            this.agentType = agentType;
            return this;
        }

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder finalState(AiState finalState) {
            this.finalState = finalState;
            return this;
        }

        public Builder stateTransitions(List<AiStateTransition> stateTransitions) {
            this.stateTransitions = stateTransitions != null ? stateTransitions : List.of();
            return this;
        }

        public Builder validated(boolean validated) {
            this.validated = validated;
            return this;
        }

        public Builder criticReviewed(boolean criticReviewed) {
            this.criticReviewed = criticReviewed;
            return this;
        }

        public Builder validationNotes(String validationNotes) {
            this.validationNotes = validationNotes;
            return this;
        }

        public Builder uiActions(List<AiTutorUiAction> uiActions) {
            this.uiActions = uiActions != null ? uiActions : List.of();
            return this;
        }

        public Builder responseTimeMs(int responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder tokensUsed(int tokensUsed) {
            this.tokensUsed = tokensUsed;
            return this;
        }

        public Builder cacheHit(boolean cacheHit) {
            this.cacheHit = cacheHit;
            return this;
        }

        public Builder contextHints(Map<String, Object> contextHints) {
            this.contextHints = contextHints != null ? contextHints : Map.of();
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder thinkingSteps(List<ThinkingStep> thinkingSteps) {
            this.thinkingSteps = thinkingSteps != null ? thinkingSteps : List.of();
            return this;
        }

        public AiHarnessResponse build() {
            return new AiHarnessResponse(
                content, conversationId, messageId,
                agentType, agentName, finalState, stateTransitions,
                validated, criticReviewed, validationNotes,
                uiActions, responseTimeMs, tokensUsed, cacheHit,
                contextHints, thinkingSteps, errorCode, errorMessage
            );
        }
    }

    /**
     * Factory method cho error response
     */
    public static AiHarnessResponse error(String traceId, String errorCode, String errorMessage, int responseTimeMs) {
        return new AiHarnessResponse(
            "I apologize, but I encountered an error. Please try again.",
            null, null, null, null,
            AiState.ERROR, List.of(AiStateTransition.start(traceId)),
            false, false, null,
            List.of(), responseTimeMs, 0, false,
            Map.of(), List.of(), errorCode, errorMessage
        );
    }
}
