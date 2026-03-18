package com.thinkai.backend.dto;

import com.thinkai.backend.entity.Lesson.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Lesson type is required")
    private LessonType type;

    private String contentUrl;

    private String contentText;

    private Integer durationSeconds;
    
    // Optional, can be derived
    private Integer orderIndex;
}
