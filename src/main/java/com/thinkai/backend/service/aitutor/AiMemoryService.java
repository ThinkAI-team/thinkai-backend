package com.thinkai.backend.service.aitutor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Service
public class AiMemoryService {

    private static final int MAX_SHORT_TERM_TURNS = 10;
    private static final int SUMMARY_THRESHOLD = 8;

    private final Map<String, Deque<ConversationTurn>> shortTermMemory = new ConcurrentHashMap<>();
    private final Map<String, String> workingSummaries = new ConcurrentHashMap<>();
    private final Map<String, StructuredContext> structuredContexts = new ConcurrentHashMap<>();

    public void addTurn(String conversationId, String userMessage, String aiResponse, String agentType) {
        Deque<ConversationTurn> turns = shortTermMemory.computeIfAbsent(
                conversationId, k -> new ConcurrentLinkedDeque<>());
        
        turns.addLast(new ConversationTurn(
                LocalDateTime.now(),
                userMessage,
                aiResponse,
                agentType));
        
        while (turns.size() > MAX_SHORT_TERM_TURNS) {
            turns.pollFirst();
        }
        
        if (turns.size() >= SUMMARY_THRESHOLD && needsSummary(conversationId)) {
            generateWorkingSummary(conversationId);
        }
    }

    public List<ConversationTurn> getRecentTurns(String conversationId, int count) {
        Deque<ConversationTurn> turns = shortTermMemory.get(conversationId);
        if (turns == null) return List.of();
        
        return turns.stream()
                .skip(Math.max(0, turns.size() - count))
                .collect(Collectors.toList());
    }

    public String getWorkingSummary(String conversationId) {
        return workingSummaries.getOrDefault(conversationId, "");
    }

    public StructuredContext getStructuredContext(String conversationId) {
        return structuredContexts.getOrDefault(conversationId, new StructuredContext());
    }

    public void updateStructuredContext(String conversationId, StructuredContext context) {
        structuredContexts.put(conversationId, context);
    }

    public String buildContextPrompt(String conversationId, UserContext userContext) {
        StringBuilder prompt = new StringBuilder();
        
        String summary = getWorkingSummary(conversationId);
        if (!summary.isEmpty()) {
            prompt.append("## Tóm tắt cuộc trò chuyện trước đó:\n")
                    .append(summary)
                    .append("\n\n");
        }
        
        List<ConversationTurn> recentTurns = getRecentTurns(conversationId, 4);
        if (!recentTurns.isEmpty()) {
            prompt.append("## Cuộc trò chuyện gần đây:\n");
            for (ConversationTurn turn : recentTurns) {
                prompt.append("- User: ").append(turn.userMessage()).append("\n");
                prompt.append("- AI: ").append(turn.aiResponse()).append("\n");
            }
            prompt.append("\n");
        }
        
        if (userContext != null) {
            prompt.append("## Thông tin người dùng:\n");
            prompt.append("- Role: ").append(userContext.role()).append("\n");
            
            if (userContext.enrolledCourses() != null && !userContext.enrolledCourses().isEmpty()) {
                prompt.append("- Khóa học đã đăng ký: ")
                        .append(String.join(", ", userContext.enrolledCourses()))
                        .append("\n");
            }
            
            if (userContext.ownedCourses() != null && !userContext.ownedCourses().isEmpty()) {
                prompt.append("- Khóa học sở hữu: ")
                        .append(String.join(", ", userContext.ownedCourses()))
                        .append("\n");
            }
            
            if (userContext.hasPendingAction()) {
                prompt.append("- ⚠️ Có thao tác đang chờ xác nhận\n");
            }
        }
        
        return prompt.toString();
    }

    public void clearConversation(String conversationId) {
        shortTermMemory.remove(conversationId);
        workingSummaries.remove(conversationId);
        structuredContexts.remove(conversationId);
    }

    private boolean needsSummary(String conversationId) {
        return !workingSummaries.containsKey(conversationId);
    }

    private void generateWorkingSummary(String conversationId) {
        Deque<ConversationTurn> turns = shortTermMemory.get(conversationId);
        if (turns == null || turns.isEmpty()) return;
        
        StringBuilder summary = new StringBuilder();
        summary.append("Tổng quan cuộc trò chuyện với BiliBily:\n");
        
        Set<String> topics = new HashSet<>();
        Set<String> agents = new HashSet<>();
        
        for (ConversationTurn turn : turns) {
            if (turn.agentType() != null) {
                agents.add(turn.agentType());
            }
        }
        
        if (!agents.isEmpty()) {
            summary.append("- Agent đã sử dụng: ").append(String.join(", ", agents)).append("\n");
        }
        
        summary.append("- Tổng số tin nhắn: ").append(turns.size()).append("\n");
        
        workingSummaries.put(conversationId, summary.toString());
    }

    public record ConversationTurn(
            LocalDateTime timestamp,
            String userMessage,
            String aiResponse,
            String agentType) {}

    public record StructuredContext(
            String role,
            List<String> enrolledCourses,
            List<String> ownedCourses,
            boolean hasPendingAction,
            Map<String, Object> metadata) {
        
        public StructuredContext() {
            this("STUDENT", List.of(), List.of(), false, new HashMap<>());
        }
    }

    public record UserContext(
            String role,
            List<String> enrolledCourses,
            List<String> ownedCourses,
            boolean hasPendingAction) {}
}
