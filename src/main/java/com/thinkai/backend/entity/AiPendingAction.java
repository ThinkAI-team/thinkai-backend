package com.thinkai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_pending_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPendingAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_id", length = 64, nullable = false)
    private String conversationId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(columnDefinition = "JSON", nullable = false)
    private String payload;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public enum Status {
        PENDING,
        CONFIRMED,
        CANCELLED,
        EXPIRED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusMinutes(10);
        }
    }
}