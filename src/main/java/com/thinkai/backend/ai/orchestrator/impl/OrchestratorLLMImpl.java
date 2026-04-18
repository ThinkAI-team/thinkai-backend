package com.thinkai.backend.ai.orchestrator.impl;

import com.thinkai.backend.ai.config.AgentConfig;
import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.llm.LLMService;
import com.thinkai.backend.ai.llm.LLMService.ChatMessage;
import com.thinkai.backend.ai.llm.LLMService.LLMResponse;
import com.thinkai.backend.ai.llm.OpenRouterService;
import com.thinkai.backend.ai.orchestrator.OrchestratorLLM;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrchestratorLLMImpl implements OrchestratorLLM {

    private final LLMService llmService;
    private TokenUsage lastTokenUsage = new TokenUsage(0, 0, 0);

    public OrchestratorLLMImpl(OpenRouterService openRouterService) {
        this.llmService = openRouterService;
    }

    @Override
    public String call(AgentType agent, String userMessage, AgentConfig config) {
        try {
            Map<String, Object> context = new HashMap<>();
            
            LLMResponse response = llmService.call(agent, userMessage, new ArrayList<>(), context);
            
            lastTokenUsage = new TokenUsage(
                response.inputTokens(),
                response.outputTokens(),
                response.totalTokens()
            );
            
            log.debug("LLM call completed: agent={}, tokens={}, latency={}ms",
                agent.getCode(), response.totalTokens(), response.latencyMs());
            
            return response.content();
            
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            return "I apologize, but I encountered an error processing your request. Please try again.";
        }
    }

    @Override
    public String callWithHistory(AgentType agent, String systemPrompt, List<Map<String, Object>> messages, AgentConfig config) {
        try {
            List<ChatMessage> history = new ArrayList<>();
            
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                history.add(ChatMessage.system(systemPrompt));
            }
            
            for (Map<String, Object> msg : messages) {
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");
                if (role != null && content != null) {
                    history.add(new ChatMessage(role, content));
                }
            }
            
            Map<String, Object> context = new HashMap<>();
            
            String userMessage = "";
            if (!messages.isEmpty()) {
                userMessage = (String) messages.get(messages.size() - 1).get("content");
            }
            
            LLMResponse response = llmService.call(agent, userMessage, history, context);
            
            lastTokenUsage = new TokenUsage(
                response.inputTokens(),
                response.outputTokens(),
                response.totalTokens()
            );
            
            return response.content();
            
        } catch (Exception e) {
            log.error("LLM call with history failed: {}", e.getMessage());
            return "I apologize, but I encountered an error. Please try again.";
        }
    }

    @Override
    public void callStreaming(AgentType agent, String context, AgentConfig config, StreamCallback callback) {
        callback.onChunk("Streaming not implemented yet. Using regular call instead.");
        String response = call(agent, context, config);
        callback.onChunk(response);
        callback.onComplete();
    }

    @Override
    public TokenUsage getLastTokenUsage() {
        return lastTokenUsage;
    }
}
