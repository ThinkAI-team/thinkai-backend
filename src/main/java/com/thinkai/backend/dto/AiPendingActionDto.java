package com.thinkai.backend.dto;

import java.time.LocalDateTime;

public class AiPendingActionDto {

    private Long id;
    private String action;
    private String payload;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public AiPendingActionDto() {}

    public AiPendingActionDto(Long id, String action, String payload, String status, 
            LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.action = action;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}