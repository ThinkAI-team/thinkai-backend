package com.thinkai.backend.ai.orchestrator.impl;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.orchestrator.OrchestratorMemory;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 7: Memory Implementation
 * Tich hop: Redis (short-term) + MySQL (long-term)
 * Tam thoi dung in-memory store
 */
@Service
public class OrchestratorMemoryImpl implements OrchestratorMemory {

    // In-memory storage (se thay bang Redis/MySQL o Phase 7)
    private final Map<String, List<ConversationTurn>> conversationStore = new ConcurrentHashMap<>();
    private final Map<Long, List<UserSkill>> userSkillsStore = new ConcurrentHashMap<>();
    private final Map<Long, List<MistakeLog>> mistakeStore = new ConcurrentHashMap<>();
    private final Map<String, AiHarnessResponse> cacheStore = new ConcurrentHashMap<>();
    private final Map<Long, List<ConversationSummary>> userConversationStore = new ConcurrentHashMap<>();

    @Override
    public void saveTurn(String conversationId, Long userId, String userMessage, String aiResponse, AgentType agent) {
        List<ConversationTurn> turns = conversationStore.computeIfAbsent(conversationId, k -> new ArrayList<>());
        turns.add(new ConversationTurn(userMessage, aiResponse, agent, System.currentTimeMillis()));

        // Keep only last 50 turns
        while (turns.size() > 50) {
            turns.remove(0);
        }
    }

    @Override
    public List<ConversationTurn> getRecentTurns(String conversationId, int count) {
        List<ConversationTurn> turns = conversationStore.getOrDefault(conversationId, List.of());
        int start = Math.max(0, turns.size() - count);
        return turns.subList(start, turns.size());
    }

    @Override
    public void saveUserSkill(Long userId, String skillName, int proficiencyLevel) {
        List<UserSkill> skills = userSkillsStore.computeIfAbsent(userId, k -> new ArrayList<>());
        skills.removeIf(s -> s.skillName().equals(skillName));
        skills.add(new UserSkill(skillName, proficiencyLevel, System.currentTimeMillis()));
    }

    @Override
    public List<UserSkill> getUserSkills(Long userId) {
        return userSkillsStore.getOrDefault(userId, List.of());
    }

    @Override
    public void logMistake(Long userId, String mistakeType, String userInput, String correctAnswer) {
        List<MistakeLog> mistakes = mistakeStore.computeIfAbsent(userId, k -> new ArrayList<>());
        mistakes.add(new MistakeLog(mistakeType, userInput, correctAnswer, 1, System.currentTimeMillis()));
    }

    @Override
    public List<MistakeLog> getRecentMistakes(Long userId, int limit) {
        List<MistakeLog> mistakes = mistakeStore.getOrDefault(userId, List.of());
        int start = Math.max(0, mistakes.size() - limit);
        return mistakes.subList(start, mistakes.size());
    }

    @Override
    public void cacheResponse(String key, AiHarnessResponse response, Duration ttl) {
        cacheStore.put(key, response);
    }

    @Override
    public Optional<AiHarnessResponse> getCachedResponse(String key) {
        return Optional.ofNullable(cacheStore.get(key));
    }

    @Override
    public List<ConversationSummary> getConversationSummaries(Long userId, int limit) {
        List<ConversationSummary> all = userConversationStore.getOrDefault(userId, new ArrayList<>());
        // Sort by lastMessageAt descending
        all.sort((a, b) -> Long.compare(b.lastMessageAt(), a.lastMessageAt()));
        return all.stream().limit(limit).toList();
    }

    public void updateConversationSummary(Long userId, String conversationId, String title, String lastPreview) {
        List<ConversationSummary> list = userConversationStore.computeIfAbsent(userId, k -> new ArrayList<>());
        // Remove existing if present
        list.removeIf(c -> c.conversationId().equals(conversationId));
        // Add new at beginning
        list.add(0, new ConversationSummary(conversationId, title, lastPreview, System.currentTimeMillis(), 0));
        // Keep only last 50
        while (list.size() > 50) {
            list.remove(list.size() - 1);
        }
    }

    public void incrementMessageCount(Long userId, String conversationId) {
        List<ConversationSummary> list = userConversationStore.get(userId);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            ConversationSummary c = list.get(i);
            if (c.conversationId().equals(conversationId)) {
                list.set(i, new ConversationSummary(
                    c.conversationId(), c.title(), c.lastMessagePreview(),
                    c.lastMessageAt(), c.messageCount() + 1));
                break;
            }
        }
    }
}
