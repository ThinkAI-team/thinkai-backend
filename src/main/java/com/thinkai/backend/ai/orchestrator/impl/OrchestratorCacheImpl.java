package com.thinkai.backend.ai.orchestrator.impl;

import com.thinkai.backend.ai.cache.SemanticCacheService;
import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.orchestrator.OrchestratorCache;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Map;

@Slf4j
@Service
public class OrchestratorCacheImpl implements OrchestratorCache {

    private final SemanticCacheService semanticCacheService;

    public OrchestratorCacheImpl(SemanticCacheService semanticCacheService) {
        this.semanticCacheService = semanticCacheService;
    }

    @Override
    public Optional<CachedEntry> check(AiHarnessRequest request, AgentType agent) {
        Long userId = request.userId();
        String message = request.message();
        String cacheQuery = buildCacheQuery(message, request.metadata());

        if (userId == null || cacheQuery == null || cacheQuery.isBlank()) {
            return Optional.empty();
        }

        return semanticCacheService.get(cacheQuery, agent, userId)
            .map(cached -> new CachedEntry(
                cached.response(),
                cached.similarity(),
                cached.timestamp(),
                cached.originalQuery()
            ));
    }

    @Override
    public void save(AiHarnessRequest request, AiHarnessResponse response, AgentType agent) {
        Long userId = request.userId();
        String message = request.message();
        String responseContent = response.content();
        String cacheQuery = buildCacheQuery(message, request.metadata());

        if (userId != null && cacheQuery != null && responseContent != null) {
            semanticCacheService.put(cacheQuery, responseContent, agent, userId);
        }
    }

    @Override
    public void invalidate(Long userId, AgentType agent) {
        if (userId != null && agent != null) {
            semanticCacheService.invalidate(agent, userId);
        }
    }

    private String buildCacheQuery(String message, Map<String, Object> metadata) {
        if (message == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(message.trim());
        if (metadata == null || metadata.isEmpty()) {
            return sb.toString();
        }
        appendMeta(sb, metadata, "language");
        appendMeta(sb, metadata, "responseLength");
        appendMeta(sb, metadata, "communicationStyle");
        appendMeta(sb, metadata, "correctionMode");
        appendMeta(sb, metadata, "answerFormat");
        return sb.toString();
    }

    private void appendMeta(StringBuilder sb, Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return;
        }
        sb.append("\n@").append(key).append(":").append(text);
    }
}
