package com.thinkai.backend.ai.state;

/**
 * State machine cho AI Harness English Learning System
 * Kiểm soát flow từ request → response qua các bước xác định
 */
public enum AiState {
    START("Initial request received"),
    ROUTED("Agent selected by router"),
    CACHE_CHECK("Semantic cache lookup completed"),
    CONTEXT_BUILT("User context assembled"),
    LLM_CALLED("LLM invoked with agent config"),
    TOOL_CALLED("Tools executed successfully"),
    VALIDATED("Response validated"),
    CRITIC_CHECK("Quality review completed"),
    DONE("Complete - response returned"),
    ERROR("Error occurred - rollback handled");

    private final String description;

    AiState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Kiểm tra state có phải là terminal state không
     */
    public boolean isTerminal() {
        return this == DONE || this == ERROR;
    }

    /**
     * Kiểm tra state có phải là error state không
     */
    public boolean isError() {
        return this == ERROR;
    }
}
