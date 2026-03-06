package com.thinkai.backend.dto.admin;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    private String description;

    private String thumbnailUrl;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", message = "Giá không được âm")
    private BigDecimal price;

    @NotNull(message = "Instructor ID không được để trống")
    private Long instructorId;

    private Boolean isPublished = false;

    private String status; // DRAFT, PENDING, APPROVED, REJECTED
}
