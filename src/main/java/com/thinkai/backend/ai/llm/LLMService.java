package com.thinkai.backend.ai.llm;

import com.thinkai.backend.ai.config.AgentType;

import java.util.List;
import java.util.Map;

public interface LLMService {
    
    LLMResponse call(AgentType agent, String userMessage, List<ChatMessage> history, Map<String, Object> context);
    
    StreamResponse stream(AgentType agent, String userMessage, List<ChatMessage> history, Map<String, Object> context);
    
    default LLMResponse callWithConfig(LLMConfig config, String userMessage, List<ChatMessage> history) {
        throw new UnsupportedOperationException("Not implemented in default");
    }
    
    record ChatMessage(String role, String content) {
        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }
        
        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
        
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }
    }
    
    record LLMResponse(
        String content,
        String agentUsed,
        int inputTokens,
        int outputTokens,
        int totalTokens,
        long latencyMs,
        String modelUsed,
        Map<String, Object> metadata
    ) {
        public static LLMResponseBuilder builder() {
            return new LLMResponseBuilder();
        }
        
        public static class LLMResponseBuilder {
            private String content;
            private String agentUsed;
            private int inputTokens;
            private int outputTokens;
            private int totalTokens;
            private long latencyMs;
            private String modelUsed;
            private Map<String, Object> metadata;
            
            public LLMResponseBuilder content(String content) {
                this.content = content;
                return this;
            }
            
            public LLMResponseBuilder agentUsed(String agentUsed) {
                this.agentUsed = agentUsed;
                return this;
            }
            
            public LLMResponseBuilder inputTokens(int inputTokens) {
                this.inputTokens = inputTokens;
                return this;
            }
            
            public LLMResponseBuilder outputTokens(int outputTokens) {
                this.outputTokens = outputTokens;
                return this;
            }
            
            public LLMResponseBuilder totalTokens(int totalTokens) {
                this.totalTokens = totalTokens;
                return this;
            }
            
            public LLMResponseBuilder latencyMs(long latencyMs) {
                this.latencyMs = latencyMs;
                return this;
            }
            
            public LLMResponseBuilder modelUsed(String modelUsed) {
                this.modelUsed = modelUsed;
                return this;
            }
            
            public LLMResponseBuilder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }
            
            public LLMResponse build() {
                return new LLMResponse(content, agentUsed, inputTokens, outputTokens, 
                    totalTokens, latencyMs, modelUsed, metadata);
            }
        }
    }
    
    record StreamResponse(
        String content,
        boolean isComplete,
        String agentUsed,
        int totalTokens,
        long latencyMs,
        String modelUsed
    ) {}
    
    record LLMConfig(
        String model,
        double temperature,
        int maxTokens,
        Double topP,
        String stop,
        boolean stream
    ) {
        private static String DEFAULT_MODEL;
        
        public static LLMConfig DEFAULT = new LLMConfig(
            null,
            0.3,
            700,
            0.9,
            null,
            false
        );

        public static void setDefaultModel(String model) {
            DEFAULT_MODEL = model;
        }

        public static LLMConfig of(AgentType agent) {
            String model = DEFAULT_MODEL;
            return new LLMConfig(model, 0.3, 700, 0.9, null, false);
        }
    }
}
