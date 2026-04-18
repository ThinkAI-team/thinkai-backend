package com.thinkai.backend.ai.tool;

import com.thinkai.backend.ai.config.AgentType;

import java.util.List;

public record ToolDefinition(
    String name,
    String description,
    AgentType targetAgent,
    boolean mutating,
    List<ToolParameter> parameters,
    boolean requiresConfirmation
) {
    public static ToolDefinition readOnly(String name, String description, AgentType targetAgent, List<ToolParameter> params) {
        return new ToolDefinition(name, description, targetAgent, false, params, false);
    }

    public static ToolDefinition mutating(String name, String description, AgentType targetAgent, List<ToolParameter> params) {
        return new ToolDefinition(name, description, targetAgent, true, params, true);
    }

    public record ToolParameter(
        String name,
        String type,
        String description,
        boolean required
    ) {}
}
