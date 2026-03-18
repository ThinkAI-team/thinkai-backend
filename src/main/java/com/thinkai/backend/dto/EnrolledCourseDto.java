package com.thinkai.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrolledCourseDto {

    private Long courseId;
    private String title;
    private String thumbnailUrl;
    private int progressPercent;
    private long totalLessons;
    private long completedLessons;
    private LocalDateTime lastAccessedAt;
}
