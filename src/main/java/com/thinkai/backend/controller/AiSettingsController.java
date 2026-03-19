package com.thinkai.backend.controller;

import com.thinkai.backend.dto.AiSettingsDto;
import com.thinkai.backend.service.AiSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/settings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiSettingsController {

    private final AiSettingsService aiSettingsService;

    @GetMapping
    public ResponseEntity<AiSettingsDto> getSettings(Authentication authentication) {
        return ResponseEntity.ok(aiSettingsService.getSettings(authentication.getName()));
    }

    @PutMapping
    public ResponseEntity<AiSettingsDto> updateSettings(@RequestBody AiSettingsDto dto, Authentication authentication) {
        return ResponseEntity.ok(aiSettingsService.updateSettings(authentication.getName(), dto));
    }
}
