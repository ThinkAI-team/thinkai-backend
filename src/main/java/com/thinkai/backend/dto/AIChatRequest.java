package com.thinkai.backend.dto;

import lombok.Data;

@Data
public class AIChatRequest {
    private String message;
    private String context;
}
