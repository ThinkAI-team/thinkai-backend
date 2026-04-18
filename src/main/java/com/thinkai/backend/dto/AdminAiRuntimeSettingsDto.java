package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAiRuntimeSettingsDto {
    private boolean tutorEnabled;
    private boolean harnessEnabled;
    private String tutorModel;
    private String tutorFallbackModel;
    private List<String> harnessModels;
    private List<String> blockedModels;
}
