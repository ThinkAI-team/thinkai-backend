package com.thinkai.backend.dto;

import com.thinkai.backend.entity.QuestionBank;
import com.thinkai.backend.entity.enums.ExamType;
import com.thinkai.backend.entity.enums.Part;
import com.thinkai.backend.entity.enums.Section;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminQuestionBankResponse {

    private Long id;
    private ExamType examType;
    private Section section;
    private Part part;
    private String content;
    private String options;
    private String correctAnswer;
    private String explanation;
    private String audioUrl;
    private String imageUrl;
    private QuestionBank.Difficulty difficulty;
    private String tags;
    private Long createdBy;
    private LocalDateTime createdAt;
}
