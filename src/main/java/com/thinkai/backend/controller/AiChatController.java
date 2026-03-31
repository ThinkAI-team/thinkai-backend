package com.thinkai.backend.controller;

import com.thinkai.backend.dto.AiChatConversationDetailDto;
import com.thinkai.backend.dto.AiChatConversationDto;
import com.thinkai.backend.dto.AiChatRenameRequest;
import com.thinkai.backend.dto.AiAgentTraceDto;
import com.thinkai.backend.dto.AiPendingActionDto;
import com.thinkai.backend.entity.AiChatLog;
import com.thinkai.backend.service.AITutorService;
import com.thinkai.backend.service.aitutor.AiPendingActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiChatController {

    private final AITutorService aiTutorService;
    private final AiPendingActionService aiPendingActionService;

    @GetMapping("/history")
    public ResponseEntity<List<AiChatConversationDto>> getChatHistory(Authentication authentication) {
        return ResponseEntity.ok(aiTutorService.getChatHistory(authentication.getName()));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<AiChatConversationDetailDto> getChatById(
            @PathVariable String conversationId,
            Authentication authentication) {
        return ResponseEntity.ok(aiTutorService.getChatByConversationId(conversationId, authentication.getName()));
    }

    @GetMapping("/{conversationId}/traces")
    public ResponseEntity<List<AiAgentTraceDto>> getConversationTraces(
            @PathVariable String conversationId,
            Authentication authentication) {
        return ResponseEntity.ok(aiTutorService.getConversationTraces(conversationId, authentication.getName()));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteChat(@PathVariable String conversationId, Authentication authentication) {
        aiTutorService.deleteConversation(conversationId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{conversationId}/title")
    public ResponseEntity<AiChatConversationDetailDto> renameConversation(
            @PathVariable String conversationId,
            @jakarta.validation.Valid @RequestBody AiChatRenameRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                aiTutorService.renameConversation(conversationId, request.getTitle(), authentication.getName()));
    }

    @PostMapping("/{messageId}/feedback")
    public ResponseEntity<AiChatLog> submitFeedback(
            @PathVariable Long messageId,
            @jakarta.validation.Valid @RequestBody com.thinkai.backend.dto.AiFeedbackRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(aiTutorService.submitFeedback(messageId, authentication.getName(), request));
    }

    @GetMapping("/{conversationId}/pending-action")
    public ResponseEntity<AiPendingActionDto> getPendingAction(
            @PathVariable String conversationId,
            Authentication authentication) {
        return ResponseEntity.ok(aiPendingActionService.getPendingActionDto(
                conversationId, authentication.getName()));
    }

    @PostMapping("/{conversationId}/confirm-action")
    public ResponseEntity<Map<String, Object>> confirmPendingAction(
            @PathVariable String conversationId,
            Authentication authentication) {
        return ResponseEntity.ok(aiPendingActionService.confirmAndExecute(
                conversationId, authentication.getName()));
    }

    @PostMapping("/{conversationId}/cancel-action")
    public ResponseEntity<Void> cancelPendingAction(
            @PathVariable String conversationId,
            Authentication authentication) {
        aiPendingActionService.cancelPendingAction(conversationId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
