package com.thinkai.backend.entity;

import com.thinkai.backend.entity.enums.ExamType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private ExamType examType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "time_limit_minutes", nullable = false)
    @Builder.Default
    private Integer timeLimitMinutes = 120; // TOEIC: 120 minutes

    @Column(name = "passing_score", nullable = false)
    @Builder.Default
    private Integer passingScore = 60;

    @Column(name = "is_random_order", nullable = false)
    @Builder.Default
    private Boolean isRandomOrder = false;

    @Column(name = "part_config", columnDefinition = "JSON")
    private String partConfig; // {"PART_1": 6, "PART_2": 25, ...}

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
