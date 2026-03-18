package com.thinkai.backend.dto;

import com.thinkai.backend.entity.enums.ExamType;
import com.thinkai.backend.entity.enums.Part;
import com.thinkai.backend.entity.enums.Section;
import com.thinkai.backend.entity.QuestionBank.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionBankRequest {

    @NotNull(message = "Exam type is required")
    private ExamType examType;

    @NotNull(message = "Section is required")
    private Section section;

    @NotNull(message = "Part is required")
    private Part part;

    @NotBlank(message = "Content is required")
    private String content;

    private String options; // JSON string representing options

    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    private String explanation;

    private String audioUrl;

    private String imageUrl;

    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;

    private List<String> tags;
}
