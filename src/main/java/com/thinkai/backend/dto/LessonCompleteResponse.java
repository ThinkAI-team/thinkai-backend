package com.thinkai.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonCompleteResponse {

    private Long lessonId;
    private Boolean isCompleted;
    private Double courseProgress;
}
