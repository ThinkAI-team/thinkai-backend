package com.thinkai.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptsRequest {

    @NotBlank(message = "Tutor system prompt không được để trống")
    private String tutorSystemPrompt;

    @NotBlank(message = "Exam generator prompt không được để trống")
    private String examGeneratorPrompt;
}
