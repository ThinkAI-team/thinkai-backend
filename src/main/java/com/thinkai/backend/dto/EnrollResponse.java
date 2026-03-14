package com.thinkai.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollResponse {

    private Long enrollmentId;
    private Long courseId;
    private LocalDateTime enrolledAt;
}
