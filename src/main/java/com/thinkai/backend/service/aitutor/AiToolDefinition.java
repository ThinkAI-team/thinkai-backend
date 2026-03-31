package com.thinkai.backend.service.aitutor;

import com.thinkai.backend.entity.User;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;

public record AiToolDefinition(
        String action,
        AiAgentType agentType,
        boolean mutating,
        Set<User.Role> allowedRoles,
        boolean requiresConfirmation,
        List<String> requiredFields,
        String ownershipCheckField) {

    public static AiToolDefinition readOnly(String action, AiAgentType agentType, Set<User.Role> allowedRoles) {
        return new AiToolDefinition(action, agentType, false, allowedRoles, false, List.of(), null);
    }

    public static AiToolDefinition mutating(String action, AiAgentType agentType, Set<User.Role> allowedRoles, 
            boolean requiresConfirmation, List<String> requiredFields) {
        return new AiToolDefinition(action, agentType, true, allowedRoles, requiresConfirmation, requiredFields, null);
    }

    public static AiToolDefinition mutatingWithOwnership(String action, AiAgentType agentType, Set<User.Role> allowedRoles, 
            boolean requiresConfirmation, List<String> requiredFields, String ownershipCheckField) {
        return new AiToolDefinition(action, agentType, true, allowedRoles, requiresConfirmation, requiredFields, ownershipCheckField);
    }

    public boolean isAllowed(User.Role role) {
        return allowedRoles == null || allowedRoles.isEmpty() || allowedRoles.contains(role);
    }

    public boolean hasRequiredFields(JsonNode args) {
        if (requiredFields == null || requiredFields.isEmpty()) {
            return true;
        }
        for (String field : requiredFields) {
            if (args == null || !args.has(field) || args.get(field).isNull()) {
                return false;
            }
        }
        return true;
    }
}