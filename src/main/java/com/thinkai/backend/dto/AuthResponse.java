package com.thinkai.backend.dto;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String token;
    private String email;
    private String fullName;
    private String role;
    private boolean hasPassword;
    private boolean isGoogleUser;
    private String avatarUrl;
}
