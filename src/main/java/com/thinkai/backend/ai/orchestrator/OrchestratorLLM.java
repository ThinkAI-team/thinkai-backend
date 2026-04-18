package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.config.AgentConfig;
import com.thinkai.backend.ai.config.AgentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LLM Service Interface - Phase 3 implement
 * Tích hợp với OpenRouter API
 */
@Component
public interface OrchestratorLLM {

    /**
     * Call LLM với agent config
     *
     * @param agent Agent type
     * @param context Full context (system + user)
     * @param config Agent configuration
     * @return LLM response
     */
    String call(AgentType agent, String context, AgentConfig config);

    /**
     * Call với conversation history
     */
    String callWithHistory(AgentType agent, String systemPrompt, List<Map<String, Object>> messages, AgentConfig config);

    /**
     * Stream response (for long responses)
     */
    void callStreaming(AgentType agent, String context, AgentConfig config, StreamCallback callback);

    /**
     * Get token usage
     */
    TokenUsage getLastTokenUsage();

    interface StreamCallback {
        void onChunk(String chunk);
        void onComplete();
        void onError(Exception e);
    }

    record TokenUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens
    ) {}
}
