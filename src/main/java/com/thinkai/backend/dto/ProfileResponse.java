package com.thinkai.backend.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private String role;
    private LocalDateTime createdAt;
}
