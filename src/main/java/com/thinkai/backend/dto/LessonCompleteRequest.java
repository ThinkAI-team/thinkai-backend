package com.thinkai.backend.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonCompleteRequest {

    @Min(value = 0, message = "watchTimeSeconds phải >= 0")
    private Integer watchTimeSeconds = 0;
}
