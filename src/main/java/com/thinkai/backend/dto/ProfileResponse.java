package com.thinkai.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private String role;
    private LocalDateTime createdAt;
}
