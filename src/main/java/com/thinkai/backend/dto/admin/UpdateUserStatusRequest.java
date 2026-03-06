package com.thinkai.backend.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {

    @NotNull(message = "isActive không được để trống")
    private Boolean isActive;
}
