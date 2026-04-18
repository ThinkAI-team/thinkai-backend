package com.thinkai.backend.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrolledCourseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long courseId;
    private String title;
    private String thumbnailUrl;
    private int progressPercent;
    private long totalLessons;
    private long completedLessons;
    private LocalDateTime lastAccessedAt;
}
