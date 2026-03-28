package com.thinkai.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProgressResponse {

    private Long lessonId;
    private Integer watchTimeSeconds;
    private Integer currentTimeSeconds;
    private Boolean isCompleted;
    private Double lessonProgressPercent;
    private Double courseProgressPercent;
}
