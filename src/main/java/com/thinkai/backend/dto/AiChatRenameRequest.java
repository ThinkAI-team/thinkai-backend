package com.thinkai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRenameRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title length must be <= 120 characters")
    private String title;
}
