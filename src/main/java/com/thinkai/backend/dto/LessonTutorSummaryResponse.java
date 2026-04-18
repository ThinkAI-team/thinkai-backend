package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonTutorSummaryResponse {
    private Long lessonId;
    private String summary;
    private boolean transcriptUsed;
    private String sourceType;
}

