package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chi tiết kết quả từng câu hỏi.
 * Bao gồm cả đáp án đúng và giải thích (chỉ hiển thị SAU khi nộp bài).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResultDto {

    private Long questionId;
    private String content;
    private String options;
    private Integer orderIndex;
    private String selectedOption; // Câu user đã chọn
    private String correctOption; // Đáp án đúng
    private Boolean isCorrect;
    private String explanation; // Giải thích đáp án
}
