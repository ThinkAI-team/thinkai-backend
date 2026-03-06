package com.thinkai.backend.dto.admin;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCourseResponse {

    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private BigDecimal price;
    private Long instructorId;
    private Boolean isPublished;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
