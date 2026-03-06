package com.thinkai.backend.dto.admin;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String avatarUrl;
    private String phoneNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
