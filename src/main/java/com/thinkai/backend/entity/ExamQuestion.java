package com.thinkai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_questions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
