package com.thinkai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(name = "conversation_title", length = 255)
    private String conversationTitle;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "ai_response", nullable = false, columnDefinition = "TEXT")
    private String aiResponse;

    @Column(columnDefinition = "JSON")
    private String citations;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "source", length = 20)
    private String source; // "tutor" or "harness"

    @Column(name = "agent_type", length = 50)
    private String agentType; // Agent type for harness (e.g., TOEIC_READING, CONVERSATION)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
