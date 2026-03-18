package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho mỗi câu trả lời trong request nộp bài.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDto {
    private Long questionId;
    private String selectedOption;
}
