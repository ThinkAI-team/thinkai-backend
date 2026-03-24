package com.thinkai.backend.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyCourseResponse {

    private Long id;
    private String title;
    private String thumbnail;
    private BigDecimal price;
    private Integer progressPercent;
    private LocalDateTime enrolledAt;
    private NextLessonInfo nextLesson;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NextLessonInfo {
        private Long id;
        private String title;
    }
}

