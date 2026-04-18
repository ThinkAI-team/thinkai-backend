package com.thinkai.backend.dto;

import com.thinkai.backend.entity.enums.ExamType;
import com.thinkai.backend.entity.enums.Section;
import com.thinkai.backend.entity.enums.Part;
import com.thinkai.backend.entity.QuestionBank.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminQuestionBankRequest {
    private ExamType examType;
    private Section section;
    private Part part;
    private String content;
    private String options;
    private String correctAnswer;
    private String explanation;
    private String audioUrl;
    private String imageUrl;
    private Difficulty difficulty;
    private String tags;
}
