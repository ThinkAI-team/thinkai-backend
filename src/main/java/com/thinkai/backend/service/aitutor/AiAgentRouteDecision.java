package com.thinkai.backend.service.aitutor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public record AiAgentRouteDecision(AiAgentType agentType, String action, JsonNode args) {

    public boolean isNone() {
        return action == null || action.isBlank() || "none".equalsIgnoreCase(action);
    }

    public static AiAgentRouteDecision none(ObjectMapper objectMapper) {
        return new AiAgentRouteDecision(AiAgentType.TUTOR, "none", objectMapper.createObjectNode());
    }
}

