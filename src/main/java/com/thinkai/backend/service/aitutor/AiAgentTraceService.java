package com.thinkai.backend.service.aitutor;

import com.thinkai.backend.dto.AiAgentTraceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class AiAgentTraceService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentTraceService.class);
    private static final int MAX_TRACE_EVENTS = 1000;
    private final Deque<AiAgentTraceDto> traces = new ConcurrentLinkedDeque<>();

    public void traceRoute(Long userId, String conversationId, String message, AiAgentRouteDecision decision) {
        appendTrace(AiAgentTraceDto.builder()
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .conversationId(conversationId)
                .agentType(decision.agentType())
                .action(decision.action())
                .message(sanitize(message))
                .build());

        log.info("ai_route userId={} conversationId={} agent={} action={} message={}",
                userId,
                conversationId,
                decision.agentType(),
                decision.action(),
                sanitize(message));
    }

    public void traceExecution(
            Long userId,
            String conversationId,
            AiAgentType agentType,
            String action,
            boolean requiresMoreInfo,
            String result) {
        traceExecution(userId, conversationId, agentType, action, requiresMoreInfo, result, null, null, null, null);
    }

    public void traceExecution(
            Long userId,
            String conversationId,
            AiAgentType agentType,
            String action,
            boolean requiresMoreInfo,
            String result,
            Long latencyMs,
            Integer inputTokens,
            Integer outputTokens,
            String toolCallChain) {
        appendTrace(AiAgentTraceDto.builder()
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .conversationId(conversationId)
                .agentType(agentType)
                .action(action)
                .result(sanitize(result))
                .requiresMoreInfo(requiresMoreInfo)
                .latencyMs(latencyMs)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .toolCallChain(toolCallChain)
                .build());

        log.info("ai_tool_exec userId={} conversationId={} action={} need_more_info={} latencyMs={} tokens_in={} tokens_out={} result={}",
                userId,
                conversationId,
                action,
                requiresMoreInfo,
                latencyMs,
                inputTokens,
                outputTokens,
                sanitize(result));
    }

    public List<AiAgentTraceDto> getConversationTraces(String conversationId, Long requesterUserId, boolean isAdmin) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        List<AiAgentTraceDto> result = new ArrayList<>();
        for (AiAgentTraceDto trace : traces) {
            if (!conversationId.equals(trace.getConversationId())) {
                continue;
            }
            if (!isAdmin && (trace.getUserId() == null || !trace.getUserId().equals(requesterUserId))) {
                continue;
            }
            result.add(trace);
        }
        return result;
    }

    public List<AiAgentTraceDto> getAllTraces(String conversationId, Long requesterUserId, boolean isAdmin) {
        if (!isAdmin) {
            return getConversationTraces(conversationId, requesterUserId, isAdmin);
        }
        
        List<AiAgentTraceDto> result = new ArrayList<>();
        for (AiAgentTraceDto trace : traces) {
            if (conversationId != null && !conversationId.isBlank()) {
                if (!conversationId.equals(trace.getConversationId())) {
                    continue;
                }
            }
            result.add(trace);
        }
        return result;
    }

    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 240) {
            return normalized.substring(0, 240) + "...";
        }
        return normalized;
    }

    private void appendTrace(AiAgentTraceDto trace) {
        traces.addLast(trace);
        while (traces.size() > MAX_TRACE_EVENTS) {
            traces.pollFirst();
        }
    }
}
