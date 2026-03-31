package com.thinkai.backend.dto;

import java.time.LocalDateTime;

public class AiActionPreviewDto {
    private String action;
    private String actionDisplayName;
    private String description;
    private String payloadPreview;
    private boolean requiresConfirmation;
    private LocalDateTime expiresAt;
    private String confirmationPrompt;

    public AiActionPreviewDto() {}

    public AiActionPreviewDto(String action, String actionDisplayName, String description, 
            String payloadPreview, boolean requiresConfirmation, LocalDateTime expiresAt, 
            String confirmationPrompt) {
        this.action = action;
        this.actionDisplayName = actionDisplayName;
        this.description = description;
        this.payloadPreview = payloadPreview;
        this.requiresConfirmation = requiresConfirmation;
        this.expiresAt = expiresAt;
        this.confirmationPrompt = confirmationPrompt;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getActionDisplayName() { return actionDisplayName; }
    public void setActionDisplayName(String actionDisplayName) { this.actionDisplayName = actionDisplayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPayloadPreview() { return payloadPreview; }
    public void setPayloadPreview(String payloadPreview) { this.payloadPreview = payloadPreview; }
    public boolean isRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getConfirmationPrompt() { return confirmationPrompt; }
    public void setConfirmationPrompt(String confirmationPrompt) { this.confirmationPrompt = confirmationPrompt; }
}
