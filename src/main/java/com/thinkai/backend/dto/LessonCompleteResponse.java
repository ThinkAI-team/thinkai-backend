package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonCompleteResponse {
    private Long lessonId;
    private Boolean isCompleted;
    private Integer courseProgress;
}
