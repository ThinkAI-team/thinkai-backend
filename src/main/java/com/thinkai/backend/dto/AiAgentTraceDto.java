package com.thinkai.backend.dto;

import com.thinkai.backend.service.aitutor.AiAgentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiAgentTraceDto {
    private LocalDateTime createdAt;
    private Long userId;
    private String conversationId;
    private AiAgentType agentType;
    private String action;
    private String message;
    private String result;
    private Boolean requiresMoreInfo;
    private Long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private String toolCallChain;
}
