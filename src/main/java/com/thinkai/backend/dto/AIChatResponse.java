package com.thinkai.backend.dto;

import com.thinkai.backend.entity.AiPendingAction;
import com.thinkai.backend.service.aitutor.AiAgentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIChatResponse {
    private String reply;
    private String conversationId;
    private Long messageId;
    private List<AiTutorUiAction> actions;
    
    @JsonProperty("agentType")
    private AiAgentType agentType;
    
    private boolean needsMoreInfo;
    private String missingField;
    private String fieldPrompt;
    
    @JsonProperty("pendingAction")
    private AiPendingAction pendingAction;

    @JsonProperty("thinkingSteps")
    private List<Map<String, Object>> thinkingSteps;

    @JsonProperty("harnessRemainingUses")
    private Integer harnessRemainingUses;

    @JsonProperty("harnessMaxUses")
    private Integer harnessMaxUses;

    @JsonProperty("harnessUpgradeRecommended")
    private Boolean harnessUpgradeRecommended;

    public AIChatResponse(String reply) {
        this.reply = reply;
    }

    public AIChatResponse(String reply, String conversationId, Long messageId) {
        this.reply = reply;
        this.conversationId = conversationId;
        this.messageId = messageId;
    }

    public AIChatResponse(String reply, String conversationId, Long messageId, List<AiTutorUiAction> actions, AiAgentType agentType) {
        this.reply = reply;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.actions = actions;
        this.agentType = agentType;
    }
    
    // Manual setters for thinkingSteps to ensure compatibility
    public void setThinkingSteps(List<Map<String, Object>> steps) {
        this.thinkingSteps = steps;
    }
    
    // Factory method to create response with thinking steps
    public static AIChatResponse withThinkingSteps(
            String reply, 
            String conversationId, 
            Long messageId, 
            List<AiTutorUiAction> actions, 
            AiAgentType agentType,
            List<Map<String, Object>> thinkingSteps) {
        AIChatResponse response = new AIChatResponse(reply, conversationId, messageId, actions, agentType);
        response.setThinkingSteps(thinkingSteps);
        return response;
    }
}
