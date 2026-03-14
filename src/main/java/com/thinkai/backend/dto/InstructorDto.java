package com.thinkai.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorDto {

    private Long id;
    private String fullName;
    private String avatarUrl;
}
