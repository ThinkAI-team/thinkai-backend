package com.thinkai.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSummaryDto {

    private Long id;
    private String title;
    private String type;
    private String duration;
    private Boolean isCompleted;
    private Integer orderIndex;
}
