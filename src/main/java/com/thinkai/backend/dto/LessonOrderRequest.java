package com.thinkai.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonOrderRequest {

    @NotEmpty(message = "Lesson order updates cannot be empty")
    @Valid
    private List<LessonOrderUpdate> lessonOrders;
}
