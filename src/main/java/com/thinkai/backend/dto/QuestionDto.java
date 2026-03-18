package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho câu hỏi - KHÔNG chứa correctOption và explanation
 * để chống gian lận khi đang làm bài thi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {

    private Long id;
    private String content;
    private String options; // JSON array: ["Option A", "Option B", "Option C", "Option D"]
    private String type; // SINGLE_CHOICE hoặc MULTIPLE_CHOICE
    private Integer orderIndex;
}
