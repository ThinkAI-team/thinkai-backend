package com.thinkai.backend.dto.admin;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPromptsResponse {

    private String tutorSystemPrompt;
    private String examGeneratorPrompt;
    private LocalDateTime updatedAt;
}
