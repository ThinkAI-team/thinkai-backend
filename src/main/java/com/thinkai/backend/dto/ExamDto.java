package com.thinkai.backend.dto;

import com.thinkai.backend.entity.enums.ExamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDto {

    private Long id;
    private ExamType examType;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private LocalDateTime createdAt;
}
