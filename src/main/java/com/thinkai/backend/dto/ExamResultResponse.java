package com.thinkai.backend.dto;

import com.thinkai.backend.entity.enums.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response chi tiết kết quả bài thi.
 * Bao gồm tổng quan + chi tiết từng câu trả lời.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultResponse {

    private Long attemptId;
    private String examTitle;
    private ExamType examType;
    private BigDecimal score;
    private Integer correctCount;
    private Integer totalQuestions;
    private Boolean isPassed;
    private Integer passingScore;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Long timeTakenSeconds;
    private String aiFeedback;             // Nullable — sẽ có khi tích hợp Gemini AI
    private List<AnswerResultDto> answers;  // Chi tiết từng câu
}
