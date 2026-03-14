package com.thinkai.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonDetailResponse {

    private Long id;
    private String title;
    private String type;
    private String contentUrl;
    private String contentText;
    private Integer durationSeconds;
    private Integer orderIndex;
    private String courseTitle;
    private Long courseId;
    private Boolean isCompleted;
    private Integer watchTimeSeconds;
}
