package com.thinkai.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfLessonResponse {

    private Long lessonId;
    private String title;
    private String contentUrl;
    private Integer totalPages;
    private Long courseId;
    private Integer orderIndex;
}
