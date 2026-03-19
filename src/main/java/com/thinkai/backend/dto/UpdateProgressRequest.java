package com.thinkai.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressRequest {

    @NotNull(message = "watchTimeSeconds là bắt buộc")
    @Min(value = 0, message = "watchTimeSeconds phải >= 0")
    private Integer watchTimeSeconds;
}
