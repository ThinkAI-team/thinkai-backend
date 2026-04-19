package com.thinkai.backend.ai.memory;

import com.thinkai.backend.repository.AiChatLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("checkstyle:ConstantName")
@Service
public class ConversationMemory {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemory.class);
    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final AiChatLogRepository chatLogRepository;

    public ConversationMemory(AiChatLogRepository chatLogRepository) {
        this.chatLogRepository = chatLogRepository;
    }

    public void saveMessage(Long userId, String conversationId, String message, String response, int tokens, int latencyMs) {
        log.debug("Saving chat: userId={}, convId={}, msg={}", userId, conversationId,
            message != null ? message.substring(0, Math.min(20, message.length())) : "");
    }

    public List<?> getHistory(Long userId, String conversationId, int limit) {
        try {
            return chatLogRepository.findByUserIdAndConversationIdOrderByCreatedAtAsc(userId, conversationId);
        } catch (Exception e) {
            log.error("Failed to get chat history: {}", e.getMessage());
            return List.of();
        }
    }

    public List<?> getHistory(Long userId, String conversationId) {
        return getHistory(userId, conversationId, DEFAULT_HISTORY_LIMIT);
    }

    public void deleteConversation(Long userId, String conversationId) {
        log.info("Delete conversation: userId={}, conversationId={}", userId, conversationId);
    }

    public List<String> getUserConversations(Long userId) {
        return List.of();
    }
}
