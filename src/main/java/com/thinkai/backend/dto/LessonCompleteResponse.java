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
<<<<<<< HEAD
    private Double courseProgress;
=======
    private Integer courseProgress;
>>>>>>> origin/develop
}
