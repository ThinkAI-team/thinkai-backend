package com.thinkai.backend.ai.orchestrator;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import org.springframework.stereotype.Component;

/**
 * Context Builder Interface - Phase 8 implement
 * Xây dựng context từ user profile cho personalized learning
 */
@Component
public interface OrchestratorContextBuilder {

    /**
     * Build full context cho LLM call
     * Bao gồm: user profile, conversation history, weak/strong points
     *
     * @param request User request
     * @param agent Selected agent
     * @return Context string để inject vào prompt
     */
    String build(AiHarnessRequest request, AgentType agent);

    /**
     * Build user profile summary
     */
    UserProfileContext buildUserProfile(Long userId);

    /**
     * Build conversation context
     */
    String buildConversationContext(String conversationId, int maxTurns);

    /**
     * User profile context
     */
    record UserProfileContext(
        String level,           // A1, A2, B1, B2, C1, C2
        String targetExam,      // TOEIC, IELTS
        Integer targetScore,
        java.util.List<String> weakPoints,
        java.util.List<String> strongPoints,
        int dailyStreak,
        int totalLessonsCompleted,
        java.util.Map<String, Object> metadata
    ) {}
}
