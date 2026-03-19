package com.thinkai.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonOrderUpdate {

    @NotNull
    private Long lessonId;

    @NotNull
    private Integer orderIndex;
}
