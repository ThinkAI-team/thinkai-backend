package com.thinkai.backend.dto;

import com.thinkai.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private String fullName;
    private String email;
    private User.Role role;
    private Boolean isActive;
}
