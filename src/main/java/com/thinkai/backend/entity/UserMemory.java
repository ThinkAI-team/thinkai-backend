package com.thinkai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_memory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "user_level", length = 20)
    private String userLevel; // A1, A2, B1, B2, C1, C2

    @Column(name = "target_exam", length = 50)
    private String targetExam; // TOEIC, IELTS,...

    @Column(name = "target_score")
    private Integer targetScore;

    @Column(name = "weak_points", columnDefinition = "TEXT")
    private String weakPoints; // JSON array hoặc comma-separated

    @Column(name = "strong_points", columnDefinition = "TEXT")
    private String strongPoints;

    @Column(name = "lesson_context", length = 100)
    private String lessonContext;

    @Column(name = "adaptive_rules", columnDefinition = "TEXT")
    private String adaptiveRules;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
