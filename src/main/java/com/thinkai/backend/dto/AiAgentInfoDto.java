package com.thinkai.backend.dto;

import com.thinkai.backend.service.aitutor.AiAgentType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiAgentInfoDto {
    private AiAgentType currentAgent;
    private String agentDisplayName;
    private String agentDescription;
    private boolean canExecute;
    private List<String> availableActions;
    private List<String> requiredPermissions;
}
