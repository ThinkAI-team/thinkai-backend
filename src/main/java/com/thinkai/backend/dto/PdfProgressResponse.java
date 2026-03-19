package com.thinkai.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfProgressResponse {

    private Long lessonId;
    private Integer currentPage;
    private Integer totalPages;
    private Double readingPercentage;
    private Boolean isCompleted;
    private LocalDateTime lastAccessedAt;
}
