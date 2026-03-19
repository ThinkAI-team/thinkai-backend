package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response trả về sau khi chấm bài thi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmitResponse {

    private Long attemptId;
    private String examTitle;
    private BigDecimal score;
    private Integer correctCount;
    private Integer totalQuestions;
    private Boolean isPassed;
    private Integer passingScore;
    private LocalDateTime submittedAt;
    private Long timeTakenSeconds; // Thời gian làm bài (giây)
}
