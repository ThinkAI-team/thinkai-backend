package com.thinkai.backend.dto;

import com.thinkai.backend.entity.enums.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotBlank(message = "Title is required")
    private String title;
    
    @NotNull(message = "Exam type is required")
    private ExamType examType;

    private String description;

    @NotNull(message = "Time limit is required")
    private Integer timeLimitMinutes;

    @NotNull(message = "Passing score is required")
    private Integer passingScore;

    private Boolean isRandomOrder;

    // e.g. {"PART_1": 6, "PART_2": 25, "PART_5": 30}
    private Map<String, Integer> partConfig;
}
