package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDetailResponse {
    private Long id;
    private String title;
    private String type;
    private String contentUrl;
    private String contentText;
    private Integer durationSeconds;
    private Integer orderIndex;
    
    // User progress details
    private Boolean isCompleted;
    private Integer watchTimeSeconds;
    
    // Navigation info
    private Long courseId;
    private String courseTitle;
    private Long previousLessonId;
    private Long nextLessonId;
}
