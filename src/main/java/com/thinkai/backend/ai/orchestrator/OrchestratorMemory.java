package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Memory Interface - Phase 7 implement
 * Lưu trữ conversation history và user progress
 * Tích hợp: Redis (short-term) + MySQL (long-term)
 */
@Component
public interface OrchestratorMemory {

    /**
     * Save conversation turn
     */
    void saveTurn(String conversationId, Long userId, String userMessage, String aiResponse, AgentType agent);

    /**
     * Get recent conversation turns
     */
    List<ConversationTurn> getRecentTurns(String conversationId, int count);

    /**
     * Save user skill assessment
     */
    void saveUserSkill(Long userId, String skillName, int proficiencyLevel);

    /**
     * Get user skills
     */
    List<UserSkill> getUserSkills(Long userId);

    /**
     * Log mistake
     */
    void logMistake(Long userId, String mistakeType, String userInput, String correctAnswer);

    /**
     * Get recent mistakes
     */
    List<MistakeLog> getRecentMistakes(Long userId, int limit);

    /**
     * Cache response (short-term)
     */
    void cacheResponse(String key, AiHarnessResponse response, Duration ttl);

    /**
     * Get cached response
     */
    Optional<AiHarnessResponse> getCachedResponse(String key);

    /**
     * Get all conversation summaries for a user
     */
    List<ConversationSummary> getConversationSummaries(Long userId, int limit);

    // Record classes
    record ConversationTurn(
        String userMessage,
        String aiResponse,
        AgentType agentType,
        long timestamp
    ) {}

    record UserSkill(
        String skillName,
        int proficiencyLevel,  // 0-100
        long lastPracticed
    ) {}

    record MistakeLog(
        String mistakeType,
        String userInput,
        String correctAnswer,
        int timesMade,
        long lastMade
    ) {}

    record ConversationSummary(
        String conversationId,
        String title,
        String lastMessagePreview,
        long lastMessageAt,
        int messageCount
    ) {}
}
