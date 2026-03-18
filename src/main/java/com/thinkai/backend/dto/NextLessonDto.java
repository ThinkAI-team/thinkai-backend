package com.thinkai.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextLessonDto {

    private Long lessonId;
    private String lessonTitle;
    private String courseTitle;
    private String type;
}
