package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSettingsDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String language;
    private String responseLength;
    private String communicationStyle;
    private String correctionMode;
    private String answerFormat;
}
