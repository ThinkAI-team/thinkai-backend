package com.thinkai.backend.ai.context;

import com.thinkai.backend.entity.AiChatLog;
import com.thinkai.backend.repository.AiChatLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ConversationContextLoader {

    private final AiChatLogRepository chatLogRepository;
    private static final int DEFAULT_MAX_TURNS = 10;

    public ConversationContextLoader(AiChatLogRepository chatLogRepository) {
        this.chatLogRepository = chatLogRepository;
    }

    public String load(Long userId, String conversationId) {
        return load(userId, conversationId, DEFAULT_MAX_TURNS);
    }

    public String load(Long userId, String conversationId, int maxTurns) {
        if (userId == null || conversationId == null) {
            return "";
        }

        try {
            List<AiChatLog> logs = chatLogRepository
                .findByUserIdAndConversationIdOrderByCreatedAtAsc(userId, conversationId);

            if (logs == null || logs.isEmpty()) {
                return "";
            }

            List<AiChatLog> recentLogs;
            int startIndex = Math.max(0, logs.size() - maxTurns);
            recentLogs = logs.subList(startIndex, logs.size());

            StringBuilder context = new StringBuilder();
            context.append("Conversation History:\n");

            for (AiChatLog log : recentLogs) {
                String userMsg = log.getUserMessage() != null ? log.getUserMessage() : "";
                String aiMsg = log.getAiResponse() != null ? log.getAiResponse() : "";
                
                if (!userMsg.isBlank()) {
                    context.append("User: ");
                    context.append(userMsg);
                    context.append("\n");
                }
                if (!aiMsg.isBlank()) {
                    context.append("AI: ");
                    context.append(aiMsg);
                    context.append("\n");
                }
            }

            return context.toString();

        } catch (Exception e) {
            log.error("Error loading conversation context: {}", e.getMessage());
            return "";
        }
    }

    public List<ChatEntry> getRecentChats(Long userId, String conversationId, int count) {
        if (userId == null || conversationId == null) {
            return List.of();
        }

        try {
            List<AiChatLog> logs = chatLogRepository
                .findByUserIdAndConversationIdOrderByCreatedAtDesc(userId, conversationId);

            if (logs == null || logs.isEmpty()) {
                return List.of();
            }

            return logs.stream()
                .limit(count)
                .map(log -> new ChatEntry(
                    "user",
                    log.getUserMessage() != null ? log.getUserMessage() : "",
                    log.getCreatedAt()
                ))
                .toList();

        } catch (Exception e) {
            log.error("Error getting recent chats: {}", e.getMessage());
            return List.of();
        }
    }

    public record ChatEntry(String role, String message, java.time.LocalDateTime timestamp) {}
}
