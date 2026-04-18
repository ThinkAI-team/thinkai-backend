package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMemoryDto {
    private Long userId;
    private String userLevel;
    private String targetExam;
    private Integer targetScore;
    private String weakPoints;
    private String strongPoints;
    private String lessonContext;
    private String adaptiveRules;
}
