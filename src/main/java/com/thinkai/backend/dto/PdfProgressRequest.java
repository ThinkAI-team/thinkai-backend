package com.thinkai.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfProgressRequest {

    @NotNull(message = "currentPage không được để trống")
    @Min(value = 1, message = "currentPage phải >= 1")
    private Integer currentPage;

    @NotNull(message = "totalPages không được để trống")
    @Min(value = 1, message = "totalPages phải >= 1")
    private Integer totalPages;
}
