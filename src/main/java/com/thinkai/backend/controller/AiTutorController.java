package com.thinkai.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-tutor")
@RequiredArgsConstructor
public class AiTutorController {

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody String message) {
        // Mọi user có token đều có thể dùng AI Tutor
        return ResponseEntity.ok("AI response to: " + message);
    }
}
