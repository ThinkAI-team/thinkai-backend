package com.thinkai.backend.controller;

import com.thinkai.backend.dto.AIChatRequest;
import com.thinkai.backend.dto.AIChatResponse;
import com.thinkai.backend.dto.AISummarizeRequest;
import com.thinkai.backend.dto.AISummarizeResponse;
import com.thinkai.backend.service.AITutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AITutorController {

    private final AITutorService aiTutorService;

    public AITutorController(AITutorService aiTutorService) {
        this.aiTutorService = aiTutorService;
    }

    @PostMapping("/chat")
<<<<<<< HEAD
    public ResponseEntity<AIChatResponse> chat(@RequestBody AIChatRequest request, org.springframework.security.core.Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if ("anonymousUser".equals(email)) {
            email = null;
        }
        return ResponseEntity.ok(aiTutorService.chat(request, email));
=======
    public ResponseEntity<AIChatResponse> chat(@RequestBody AIChatRequest request) {
        AIChatResponse response = aiTutorService.chat(request);
        return ResponseEntity.ok(response);
>>>>>>> feature/ai-tutor/chat
    }

    @PostMapping("/summarize")
    public ResponseEntity<AISummarizeResponse> summarize(@RequestBody AISummarizeRequest request) {
        AISummarizeResponse response = aiTutorService.summarize(request);
        return ResponseEntity.ok(response);
    }
}
