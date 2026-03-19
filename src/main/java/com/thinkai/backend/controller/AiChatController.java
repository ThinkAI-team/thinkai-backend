package com.thinkai.backend.controller;

import com.thinkai.backend.entity.AiChatLog;
import com.thinkai.backend.service.AITutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiChatController {

    private final AITutorService aiTutorService;

    @GetMapping("/history")
    public ResponseEntity<List<AiChatLog>> getChatHistory(Authentication authentication) {
        return ResponseEntity.ok(aiTutorService.getChatHistory(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AiChatLog> getChatById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(aiTutorService.getChatById(id, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long id, Authentication authentication) {
        aiTutorService.deleteChat(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/feedback")
    public ResponseEntity<AiChatLog> submitFeedback(
            @PathVariable Long messageId,
            @jakarta.validation.Valid @RequestBody com.thinkai.backend.dto.AiFeedbackRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(aiTutorService.submitFeedback(messageId, authentication.getName(), request));
    }
}
