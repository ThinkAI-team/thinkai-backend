package com.thinkai.backend.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AIChatRequest {
    private String message;
    private String context;
    private String conversationId;
    private String language;
    private String responseLength;
    private String communicationStyle;
    private String correctionMode;
    private String answerFormat;
    private Map<String, Object> metadata;
}
