package com.thinkai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_answers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"attempt_id", "question_id"})
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_option", length = 10)
    private String selectedOption;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;
}
