package com.thinkai.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPreferenceDto {

    @DecimalMin(value = "0.25", message = "Tốc độ tối thiểu 0.25x")
    @DecimalMax(value = "3.0", message = "Tốc độ tối đa 3.0x")
    private Double playbackSpeed;

    private Boolean autoPlay;

    private String quality;
}
