package com.thinkai.backend.controller;

import com.thinkai.backend.dto.AIChatRequest;
import com.thinkai.backend.dto.AIChatResponse;
import com.thinkai.backend.dto.AISummarizeRequest;
import com.thinkai.backend.dto.AISummarizeResponse;
import com.thinkai.backend.service.AITutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-tutor")
@RequiredArgsConstructor
public class AiTutorController {

    private final AITutorService aiTutorService;

    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> chat(@RequestBody AIChatRequest request, org.springframework.security.core.Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if ("anonymousUser".equals(email)) {
            email = null;
        }
        return ResponseEntity.ok(aiTutorService.chat(request, email));
    }

    @PostMapping("/summarize")
    public ResponseEntity<AISummarizeResponse> summarize(@RequestBody AISummarizeRequest request) {
        return ResponseEntity.ok(aiTutorService.summarize(request));
    }
}
