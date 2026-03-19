package com.thinkai.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiFeedbackRequest {
    @NotNull(message = "Rating không được để trống")
    @Min(value = -1, message = "Rating không hợp lệ")
    @Max(value = 1, message = "Rating không hợp lệ")
    private Integer rating;
}
