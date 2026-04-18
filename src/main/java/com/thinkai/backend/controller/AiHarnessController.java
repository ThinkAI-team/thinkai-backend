package com.thinkai.backend.controller;

import com.thinkai.backend.ai.orchestrator.AiOrchestrator;
import com.thinkai.backend.ai.orchestrator.OrchestratorMemory;
import com.thinkai.backend.ai.orchestrator.adapter.AiHarnessAdapter;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import com.thinkai.backend.ai.state.AiHarnessResponse;
import com.thinkai.backend.dto.AIChatRequest;
import com.thinkai.backend.dto.AIChatResponse;
import com.thinkai.backend.dto.AiChatConversationDto;
import com.thinkai.backend.dto.AiPendingActionDto;
import com.thinkai.backend.dto.AiSettingsDto;
import com.thinkai.backend.dto.UserMemoryDto;
import com.thinkai.backend.entity.AiChatLog;
import com.thinkai.backend.entity.AiPendingAction;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.entity.UserMemory;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.AiChatLogRepository;
import com.thinkai.backend.repository.UserMemoryRepository;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.AiSettingsService;
import com.thinkai.backend.service.aitutor.AiPendingActionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/ai-harness")
@RequiredArgsConstructor
public class AiHarnessController {

    private static final Logger log = LoggerFactory.getLogger(AiHarnessController.class);
    private static final String HARNESS_SOURCE = "harness";

    private final AiOrchestrator aiOrchestrator;
    private final AiHarnessAdapter adapter;
    private final OrchestratorMemory memory;
    private final UserRepository userRepository;
    private final AiChatLogRepository aiChatLogRepository;
    private final UserMemoryRepository userMemoryRepository;
    private final AiPendingActionService aiPendingActionService;
    private final AiSettingsService aiSettingsService;
    private final ObjectMapper objectMapper;

    @Value("${app.harness.free-max-uses:10}")
    private int freeHarnessMaxUses;

    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> chat(
            @RequestBody AIChatRequest request,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : null;
        if ("anonymousUser".equals(email)) {
            email = null;
        }
        final String resolvedEmail = email;

        String message = request.getMessage();
        if (message == null || message.trim().isEmpty()) {
            throw new ApiException("Message cannot be empty", HttpStatus.BAD_REQUEST);
        }

        // Get userId from database
        Long userId = null;
        User user = null;
        HarnessQuotaStatus quotaStatus = null;
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userId = user.getId();
                quotaStatus = evaluateHarnessQuota(user);
            }
        }

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = email != null ? UUID.randomUUID().toString() : "anon-" + UUID.randomUUID().toString();
        }

        if (userId != null && isConfirmCommand(message)) {
            Map<String, Object> confirmResult = aiPendingActionService.confirmAndExecute(conversationId, email);
            String confirmMessage = confirmResult.getOrDefault("message", "Confirmed").toString();
            AIChatResponse confirmResponse = new AIChatResponse(confirmMessage, conversationId, null, List.of(), null);
            applyQuotaInfo(confirmResponse, quotaStatus, false);
            return ResponseEntity.ok(confirmResponse);
        }
        if (userId != null && isCancelCommand(message)) {
            aiPendingActionService.cancelPendingAction(conversationId, email);
            AIChatResponse cancelResponse = new AIChatResponse("Đã hủy hành động đang chờ xác nhận.", conversationId, null, List.of(), null);
            applyQuotaInfo(cancelResponse, quotaStatus, false);
            return ResponseEntity.ok(cancelResponse);
        }

        String userLevel;
        String lessonContext;
        
        if (userId != null) {
            Optional<UserMemory> userMemoryOpt = userMemoryRepository.findByUserId(userId);
            if (userMemoryOpt.isPresent()) {
                UserMemory um = userMemoryOpt.get();
                userLevel = um.getUserLevel() != null ? um.getUserLevel() : "B1";
                lessonContext = um.getTargetExam() != null 
                    ? um.getTargetExam() + (um.getTargetScore() != null ? " Target " + um.getTargetScore() : "")
                    : "General English";
            } else {
                userLevel = "B1";
                lessonContext = "General English";
            }
        } else {
            userLevel = "B1";
            lessonContext = "General English";
        }

        AiHarnessRequest harnessRequest = AiHarnessRequest.builder()
            .traceId(UUID.randomUUID().toString())
            .userId(userId)
            .conversationId(conversationId)
            .message(message)
            .userLevel(userLevel)
            .lessonContext(lessonContext)
            .metadata(buildChatMetadata(request, email))
            .build();

        AiHarnessResponse harnessResponse = aiOrchestrator.handle(harnessRequest);

        // Save to AiChatLog (MySQL) for persistent storage
        if (userId != null) {
            try {
                String conversationTitle = generateConversationTitle(message);
                AiChatLog chatLog = AiChatLog.builder()
                    .userId(userId)
                    .conversationId(conversationId)
                    .conversationTitle(conversationTitle)
                    .userMessage(message)
                    .aiResponse(harnessResponse.content() != null ? harnessResponse.content() : "")
                    .responseTimeMs(harnessResponse.responseTimeMs())
                    .source(HARNESS_SOURCE)
                    .agentType(harnessResponse.agentType() != null ? harnessResponse.agentType().getCode() : null)
                    .build();
                aiChatLogRepository.save(chatLog);
                log.debug("Saved harness conversation to AiChatLog: {}", conversationId);
            } catch (Exception e) {
                log.warn("Failed to save harness conversation to database: {}", e.getMessage());
            }
        }

        AIChatResponse response = adapter.toChatResponse(harnessResponse);
        applyQuotaInfo(response, quotaStatus, true);

        tryAttachPendingAction(response, harnessResponse, userId, conversationId);
        return ResponseEntity.ok(response);
    }

    private void tryAttachPendingAction(AIChatResponse response, AiHarnessResponse harnessResponse, Long userId, String conversationId) {
        if (response == null || harnessResponse == null || userId == null) {
            return;
        }
        Map<String, Object> hints = harnessResponse.contextHints();
        if (hints == null) {
            return;
        }
        Object required = hints.get("confirmationRequired");
        if (!(required instanceof Boolean requiredBool) || !requiredBool) {
            return;
        }
        Object actionObj = hints.get("pendingAction");
        Object argsObj = hints.get("pendingArgs");
        if (!(actionObj instanceof String action) || action.isBlank()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(argsObj != null ? argsObj : Map.of());
            AiPendingAction pendingAction = aiPendingActionService.create(userId, conversationId, action, payload);
            response.setPendingAction(pendingAction);
        } catch (Exception e) {
            log.warn("Failed to create pending action for harness: {}", e.getMessage());
        }
    }

    private boolean isConfirmCommand(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.trim().toLowerCase();
        return lower.equals("xác nhận") || lower.equals("xac nhan") || lower.equals("confirm") || lower.equals("ok");
    }

    private boolean isCancelCommand(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.trim().toLowerCase();
        return lower.equals("hủy") || lower.equals("huỷ") || lower.equals("cancel") || lower.equals("không");
    }

    private String generateConversationTitle(String message) {
        if (message == null || message.isEmpty()) {
            return "New Conversation";
        }
        String[] words = message.split("\\s+");
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < Math.min(5, words.length); i++) {
            if (i > 0) title.append(" ");
            title.append(words[i]);
        }
        return title.length() > 30 ? title.substring(0, 27) + "..." : title.toString();
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestBody AIChatRequest request,
            Authentication authentication) {

        String email = authentication != null ? authentication.getName() : null;
        if ("anonymousUser".equals(email)) {
            email = null;
        }
        final String resolvedEmail = email;

        User streamUser = null;
        if (resolvedEmail != null) {
            streamUser = userRepository.findByEmail(resolvedEmail).orElse(null);
            if (streamUser != null) {
                evaluateHarnessQuota(streamUser);
            }
        }

        String message = request.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("error:Message cannot be empty");
        }

        String conversationId = request.getConversationId();
        String finalConversationId;
        if (conversationId == null || conversationId.isBlank()) {
            finalConversationId = resolvedEmail != null ? UUID.randomUUID().toString() : "anon-" + UUID.randomUUID().toString();
        } else {
            finalConversationId = conversationId;
        }

        log.info("[STREAM] Starting stream for conversation: {}", finalConversationId);

        // Step-by-step stream
        return Mono.fromCallable(() -> {
            AiHarnessRequest harnessRequest = AiHarnessRequest.builder()
                .traceId(UUID.randomUUID().toString())
                .userId(null)
                .conversationId(finalConversationId)
                .message(message)
                .userLevel("B1")
                .lessonContext("General English")
                .metadata(buildChatMetadata(request, resolvedEmail))
                .build();

            return aiOrchestrator.handle(harnessRequest);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(response -> {
            // Stream each thinking step
            var steps = response.thinkingSteps();
            if (steps == null || steps.isEmpty()) {
                return Flux.just("{\"type\":\"done\",\"content\":\"" + escapeJson(response.content()) + "\"}");
            }

            AtomicInteger index = new AtomicInteger(0);
            return Flux.fromIterable(steps)
                .flatMap(step -> {
                    int currentIndex = index.getAndIncrement();
                    // Add delay for visual effect
                    return Mono.delay(Duration.ofMillis(currentIndex * 200))
                        .map(tick -> {
                            String stepJson = String.format(
                                "{\"type\":\"step\",\"index\":%d,\"step\":\"%s\",\"description\":\"%s\",\"latencyMs\":%d,\"success\":%b}",
                                currentIndex,
                                escapeJson(step.step()),
                                escapeJson(step.description()),
                                step.latencyMs(),
                                step.success()
                            );
                            log.debug("[STREAM] Sending step {}: {}", currentIndex, step.step());
                            return stepJson;
                        });
                })
                .concatWith(Mono.delay(Duration.ofMillis(300))
                    .map(tick -> {
                        String finalJson = String.format(
                            "{\"type\":\"done\",\"content\":\"%s\",\"conversationId\":\"%s\"}",
                            escapeJson(response.content()),
                            response.conversationId() != null ? response.conversationId() : finalConversationId
                        );
                        log.info("[STREAM] Stream complete for: {}", finalConversationId);
                        return finalJson;
                    }));
        })
        .onErrorResume(e -> {
            log.error("[STREAM] Error: {}", e.getMessage());
            return Flux.just("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        });
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private HarnessQuotaStatus evaluateHarnessQuota(User user) {
        HarnessQuotaSnapshot snapshot = getHarnessQuotaSnapshot(user);
        if (snapshot == null) {
            return null;
        }
        if (snapshot.exhausted()) {
            throw new ApiException(
                    "Gói Free chỉ dùng AI Harness tối đa " + freeHarnessMaxUses
                            + " lần. Bạn đã dùng " + snapshot.used() + "/" + freeHarnessMaxUses + ".",
                    HttpStatus.PAYMENT_REQUIRED);
        }
        return new HarnessQuotaStatus(snapshot.maxUses(), snapshot.remaining());
    }

    private void applyQuotaInfo(AIChatResponse response, HarnessQuotaStatus quotaStatus, boolean consumeOneUse) {
        if (response == null || quotaStatus == null) {
            return;
        }
        int remaining = quotaStatus.remainingBefore();
        if (consumeOneUse) {
            remaining = Math.max(0, remaining - 1);
        }
        response.setHarnessMaxUses(quotaStatus.maxUses());
        response.setHarnessRemainingUses(remaining);
        response.setHarnessUpgradeRecommended(remaining <= 1);
    }

    private record HarnessQuotaStatus(int maxUses, int remainingBefore) {
    }

    private HarnessQuotaSnapshot getHarnessQuotaSnapshot(User user) {
        if (user == null || user.getId() == null || user.getRole() != User.Role.STUDENT) {
            return null;
        }
        if (freeHarnessMaxUses <= 0) {
            return null;
        }
        long used = aiChatLogRepository.countByUserIdAndSource(user.getId(), HARNESS_SOURCE);
        int remaining = (int) Math.max(0, freeHarnessMaxUses - used);
        boolean exhausted = used >= freeHarnessMaxUses;
        return new HarnessQuotaSnapshot(freeHarnessMaxUses, used, remaining, exhausted);
    }

    private record HarnessQuotaSnapshot(int maxUses, long used, int remaining, boolean exhausted) {
    }

    @GetMapping("/quota")
    public ResponseEntity<Map<String, Object>> getHarnessQuota(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        HarnessQuotaSnapshot snapshot = getHarnessQuotaSnapshot(user);
        if (snapshot == null) {
            return ResponseEntity.ok(Map.of(
                    "maxUses", 0,
                    "used", 0,
                    "remaining", 0,
                    "exhausted", false,
                    "upgradeRecommended", false));
        }
        return ResponseEntity.ok(Map.of(
                "maxUses", snapshot.maxUses(),
                "used", snapshot.used(),
                "remaining", snapshot.remaining(),
                "exhausted", snapshot.exhausted(),
                "upgradeRecommended", snapshot.remaining() <= 1));
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<AiChatConversationDto>> getHistory(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            return ResponseEntity.ok(List.of());
        }
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        
        // Get harness conversations from AiChatLog (source = "harness")
        List<AiChatLog> logs = aiChatLogRepository.findDistinctByUserIdAndSourceOrderByCreatedAtDesc(user.getId(), "harness");
        
        // Group by conversationId
        Map<String, List<AiChatLog>> grouped = logs.stream()
            .collect(java.util.stream.Collectors.groupingBy(AiChatLog::getConversationId));
        
        List<AiChatConversationDto> result = grouped.entrySet().stream()
            .map(entry -> {
                String convId = entry.getKey();
                List<AiChatLog> convLogs = entry.getValue();
                AiChatLog first = convLogs.get(0);
                AiChatLog last = convLogs.get(convLogs.size() - 1);
                
                return AiChatConversationDto.builder()
                    .conversationId(convId)
                    .title(first.getConversationTitle() != null ? first.getConversationTitle() : "Conversation " + convId.substring(0, 8))
                    .lastMessagePreview(last.getUserMessage() != null && last.getUserMessage().length() > 100 
                        ? last.getUserMessage().substring(0, 100) + "..." 
                        : last.getUserMessage())
                    .lastMessageAt(last.getCreatedAt())
                    .messageCount(convLogs.size())
                    .build();
            })
            .sorted((a, b) -> b.getLastMessageAt().compareTo(a.getLastMessageAt()))
            .limit(50)
            .toList();
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{conversationId}/pending-action")
    public ResponseEntity<AiPendingActionDto> getPendingAction(
            @PathVariable String conversationId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(aiPendingActionService.getPendingActionDto(conversationId, email));
    }

    @PostMapping("/actions/confirm")
    public ResponseEntity<Map<String, Object>> confirmAction(
            @RequestBody(required = false) Map<String, Object> payload,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }

        Long actionId = payload != null ? toLong(payload.get("actionId")) : null;
        String conversationId = payload != null ? toString(payload.get("conversationId")) : null;

        Map<String, Object> result;
        if (actionId != null) {
            result = aiPendingActionService.confirmAndExecuteById(actionId, email);
        } else if (conversationId != null && !conversationId.isBlank()) {
            result = aiPendingActionService.confirmAndExecute(conversationId, email);
        } else {
            throw new ApiException("actionId or conversationId is required", HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/actions/cancel")
    public ResponseEntity<Map<String, Object>> cancelAction(
            @RequestBody(required = false) Map<String, Object> payload,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }

        Long actionId = payload != null ? toLong(payload.get("actionId")) : null;
        String conversationId = payload != null ? toString(payload.get("conversationId")) : null;

        if (actionId != null) {
            return ResponseEntity.ok(aiPendingActionService.cancelPendingActionById(actionId, email));
        }
        if (conversationId != null && !conversationId.isBlank()) {
            aiPendingActionService.cancelPendingAction(conversationId, email);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pending action cancelled"));
        }
        throw new ApiException("actionId or conversationId is required", HttpStatus.BAD_REQUEST);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toString(Object value) {
        return value != null ? value.toString() : null;
    }
    
    @GetMapping("/chat/{conversationId}")
    public ResponseEntity<AiChatConversationDto> getConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new ApiException("User not found", HttpStatus.NOT_FOUND);
        }
        
        List<AiChatLog> logs = aiChatLogRepository.findByUserIdAndSourceAndConversationIdOrderByCreatedAtAsc(user.getId(), "harness", conversationId);
        
        if (logs.isEmpty()) {
            throw new ApiException("Conversation not found", HttpStatus.NOT_FOUND);
        }
        
        AiChatLog first = logs.get(0);
        AiChatLog last = logs.get(logs.size() - 1);
        
        AiChatConversationDto dto = AiChatConversationDto.builder()
            .conversationId(conversationId)
            .title(first.getConversationTitle() != null ? first.getConversationTitle() : "Conversation " + conversationId.substring(0, 8))
            .lastMessagePreview(last.getUserMessage() != null && last.getUserMessage().length() > 100 
                ? last.getUserMessage().substring(0, 100) + "..." 
                : last.getUserMessage())
            .lastMessageAt(last.getCreatedAt())
            .messageCount(logs.size())
            .build();
        
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/chat/{conversationId}/messages")
    public ResponseEntity<List<AiChatLog>> getConversationMessages(
            @PathVariable String conversationId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new ApiException("User not found", HttpStatus.NOT_FOUND);
        }
        
        List<AiChatLog> logs = aiChatLogRepository.findByUserIdAndSourceAndConversationIdOrderByCreatedAtAsc(user.getId(), "harness", conversationId);
        
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/memory")
    public ResponseEntity<UserMemoryDto> getUserMemory(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            return ResponseEntity.ok(UserMemoryDto.builder()
                .build());
        }
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(UserMemoryDto.builder()
                .build());
        }
        
        UserMemory memory = userMemoryRepository.findByUserId(user.getId()).orElse(null);
        
        if (memory == null) {
            return ResponseEntity.ok(UserMemoryDto.builder()
                .userId(user.getId())
                .build());
        }
        
        return ResponseEntity.ok(UserMemoryDto.builder()
            .userId(memory.getUserId())
            .userLevel(memory.getUserLevel())
            .targetExam(memory.getTargetExam())
            .targetScore(memory.getTargetScore())
            .weakPoints(memory.getWeakPoints())
            .strongPoints(memory.getStrongPoints())
            .lessonContext(memory.getLessonContext())
            .adaptiveRules(memory.getAdaptiveRules())
            .build());
    }
    
    @PutMapping("/memory")
    public ResponseEntity<UserMemoryDto> updateUserMemory(
            @RequestBody UserMemoryDto dto,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null || "anonymousUser".equals(email)) {
            throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new ApiException("User not found", HttpStatus.NOT_FOUND);
        }
        
        UserMemory memory = userMemoryRepository.findByUserId(user.getId()).orElse(null);
        
        if (memory == null) {
            memory = UserMemory.builder()
                .userId(user.getId())
                .userLevel(normalizeOptionalText(dto.getUserLevel()))
                .targetExam(normalizeOptionalText(dto.getTargetExam()))
                .targetScore(normalizeOptionalScore(dto.getTargetScore()))
                .weakPoints(normalizeOptionalText(dto.getWeakPoints()))
                .strongPoints(normalizeOptionalText(dto.getStrongPoints()))
                .lessonContext(normalizeOptionalText(dto.getLessonContext()))
                .adaptiveRules(normalizeOptionalText(dto.getAdaptiveRules()))
                .build();
        } else {
            // Update all fields directly so user can clear values with empty input.
            memory.setUserLevel(normalizeOptionalText(dto.getUserLevel()));
            memory.setTargetExam(normalizeOptionalText(dto.getTargetExam()));
            memory.setTargetScore(normalizeOptionalScore(dto.getTargetScore()));
            memory.setWeakPoints(normalizeOptionalText(dto.getWeakPoints()));
            memory.setStrongPoints(normalizeOptionalText(dto.getStrongPoints()));
            memory.setLessonContext(normalizeOptionalText(dto.getLessonContext()));
            memory.setAdaptiveRules(normalizeOptionalText(dto.getAdaptiveRules()));
        }
        
        memory = userMemoryRepository.save(memory);
        
        return ResponseEntity.ok(UserMemoryDto.builder()
            .userId(memory.getUserId())
            .userLevel(memory.getUserLevel())
            .targetExam(memory.getTargetExam())
            .targetScore(memory.getTargetScore())
            .weakPoints(memory.getWeakPoints())
            .strongPoints(memory.getStrongPoints())
            .lessonContext(memory.getLessonContext())
            .adaptiveRules(memory.getAdaptiveRules())
            .build());
    }

    private String normalizeOptionalText(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer normalizeOptionalScore(Integer score) {
        if (score == null || score <= 0) {
            return null;
        }
        return score;
    }

    private Map<String, Object> buildChatMetadata(AIChatRequest request, String email) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        if (request != null && request.getMetadata() != null) {
            metadata.putAll(request.getMetadata());
        }

        AiSettingsDto settings = null;
        if (email != null && !"anonymousUser".equals(email)) {
            try {
                settings = aiSettingsService.getSettings(email);
            } catch (Exception ignored) {
            }
        }

        String language = firstNonBlank(
            request != null ? request.getLanguage() : null,
            settings != null ? settings.getLanguage() : null,
            "English"
        );
        String responseLength = firstNonBlank(
            request != null ? request.getResponseLength() : null,
            settings != null ? settings.getResponseLength() : null,
            "detailed"
        );
        String communicationStyle = firstNonBlank(
            request != null ? request.getCommunicationStyle() : null,
            settings != null ? settings.getCommunicationStyle() : null,
            "friendly"
        );
        String correctionMode = firstNonBlank(
            request != null ? request.getCorrectionMode() : null,
            settings != null ? settings.getCorrectionMode() : null,
            "balanced"
        );
        String answerFormat = firstNonBlank(
            request != null ? request.getAnswerFormat() : null,
            settings != null ? settings.getAnswerFormat() : null,
            "auto"
        );

        metadata.put("language", language);
        metadata.put("responseLength", responseLength);
        metadata.put("communicationStyle", communicationStyle);
        metadata.put("correctionMode", correctionMode);
        metadata.put("answerFormat", answerFormat);
        return metadata;
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isEmpty()) {
                return candidate.trim();
            }
        }
        return null;
    }
}
