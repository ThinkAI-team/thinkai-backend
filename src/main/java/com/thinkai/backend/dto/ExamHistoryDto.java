package com.thinkai.backend.dto;

import com.thinkai.backend.entity.enums.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tóm tắt mỗi lần thi trong lịch sử.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamHistoryDto {

    private Long attemptId;
    private Long examId;
    private String examTitle;
    private ExamType examType;
    private BigDecimal score;
    private Integer correctCount;
    private Integer totalQuestions;
    private Boolean isPassed;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Long timeTakenSeconds;
}
