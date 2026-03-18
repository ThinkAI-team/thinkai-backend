package com.thinkai.backend.dto;

import lombok.*;
import java.math.BigDecimal;

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
    private String nextLessonTitle;
    private Long nextLessonId;
}
