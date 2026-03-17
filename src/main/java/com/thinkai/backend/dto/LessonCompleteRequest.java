package com.thinkai.backend.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonCompleteRequest {
    @Min(value = 0, message = "watchTimeSeconds phải >= 0")
    private Integer watchTimeSeconds = 0;
}
