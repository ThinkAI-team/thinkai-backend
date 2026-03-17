package com.thinkai.backend.dto;

<<<<<<< HEAD
import jakarta.validation.constraints.Min;
=======
>>>>>>> origin/develop
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonCompleteRequest {

<<<<<<< HEAD
    @Min(value = 0, message = "watchTimeSeconds phải >= 0")
=======
>>>>>>> origin/develop
    private Integer watchTimeSeconds = 0;
}
