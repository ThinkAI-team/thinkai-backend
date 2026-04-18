package com.thinkai.backend.dto;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextLessonDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long lessonId;
    private String lessonTitle;
    private String courseTitle;
    private String type;
}
