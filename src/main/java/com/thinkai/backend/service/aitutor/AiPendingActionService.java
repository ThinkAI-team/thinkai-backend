package com.thinkai.backend.service.aitutor;

import com.thinkai.backend.ai.cache.SemanticCacheService;
import com.thinkai.backend.dto.AiPendingActionDto;
import com.thinkai.backend.entity.AiPendingAction;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.repository.AiPendingActionRepository;
import com.thinkai.backend.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiPendingActionService {

    private final AiPendingActionRepository repository;
    private final UserRepository userRepository;
    private final AiToolExecutorService aiToolExecutorService;
    private final ObjectMapper objectMapper;
    private final SemanticCacheService semanticCacheService;

    public AiPendingActionService(
            AiPendingActionRepository repository,
            UserRepository userRepository,
            @Lazy AiToolExecutorService aiToolExecutorService,
            ObjectMapper objectMapper,
            SemanticCacheService semanticCacheService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.aiToolExecutorService = aiToolExecutorService;
        this.objectMapper = objectMapper;
        this.semanticCacheService = semanticCacheService;
    }

    @Transactional
    public AiPendingAction create(Long userId, String conversationId, String action, String payload) {
        clearPendingForConversation(userId, conversationId);

        AiPendingAction pending = AiPendingAction.builder()
                .userId(userId)
                .conversationId(conversationId)
                .action(action)
                .payload(payload)
                .status(AiPendingAction.Status.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        return repository.save(pending);
    }

    public Optional<AiPendingAction> getPendingAction(Long userId, String conversationId) {
        return repository.findTopByUserIdAndConversationIdAndStatusOrderByCreatedAtDesc(
                userId, conversationId, AiPendingAction.Status.PENDING);
    }

    public List<AiPendingAction> getPendingActions(Long userId, String conversationId) {
        return repository.findByUserIdAndConversationIdAndStatus(
                userId, conversationId, AiPendingAction.Status.PENDING);
    }

    @Transactional
    public Optional<AiPendingAction> confirm(Long actionId, Long userId) {
        Optional<AiPendingAction> opt = repository.findByIdAndUserId(actionId, userId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        AiPendingAction action = opt.get();
        if (action.getStatus() != AiPendingAction.Status.PENDING) {
            return Optional.empty();
        }

        if (action.getExpiresAt() != null && action.getExpiresAt().isBefore(LocalDateTime.now())) {
            action.setStatus(AiPendingAction.Status.EXPIRED);
            repository.save(action);
            return Optional.empty();
        }

        action.setStatus(AiPendingAction.Status.CONFIRMED);
        action.setConfirmedAt(LocalDateTime.now());
        return Optional.of(repository.save(action));
    }

    @Transactional
    public Optional<AiPendingAction> cancel(Long actionId, Long userId) {
        Optional<AiPendingAction> opt = repository.findByIdAndUserId(actionId, userId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        AiPendingAction action = opt.get();
        if (action.getStatus() != AiPendingAction.Status.PENDING) {
            return Optional.empty();
        }

        action.setStatus(AiPendingAction.Status.CANCELLED);
        action.setCancelledAt(LocalDateTime.now());
        return Optional.of(repository.save(action));
    }

    @Transactional
    public void clearPendingForConversation(Long userId, String conversationId) {
        repository.deleteByUserIdAndConversationIdAndStatus(
                userId, conversationId, AiPendingAction.Status.PENDING);
    }

    public boolean hasPendingAction(Long userId, String conversationId) {
        return repository.findTopByUserIdAndConversationIdAndStatusOrderByCreatedAtDesc(
                userId, conversationId, AiPendingAction.Status.PENDING).isPresent();
    }

    public AiPendingActionDto getPendingActionDto(String conversationId, String email) {
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            return null;
        }
        User user = optUser.get();
        
        Optional<AiPendingAction> opt = getPendingAction(user.getId(), conversationId);
        if (opt.isEmpty()) {
            return null;
        }
        
        AiPendingAction pending = opt.get();
        return new AiPendingActionDto(
                pending.getId(),
                pending.getAction(),
                pending.getPayload(),
                pending.getStatus().name(),
                pending.getCreatedAt(),
                pending.getExpiresAt()
        );
    }

    @Transactional
    public Map<String, Object> confirmAndExecute(String conversationId, String email) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "User not found");
            return result;
        }
        User user = optUser.get();

        Optional<AiPendingAction> optPending = getPendingAction(user.getId(), conversationId);
        if (optPending.isEmpty()) {
            result.put("success", false);
            result.put("message", "No pending action found");
            return result;
        }

        AiPendingAction pending = optPending.get();
        
        if (pending.getExpiresAt() != null && pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            pending.setStatus(AiPendingAction.Status.EXPIRED);
            repository.save(pending);
            result.put("success", false);
            result.put("message", "Action has expired");
            return result;
        }

        try {
            JsonNode args = objectMapper.readTree(pending.getPayload());
            AiToolExecutorService.ToolExecuteResult execResult = 
                    aiToolExecutorService.execute(pending.getAction(), args, user);

            pending.setStatus(AiPendingAction.Status.CONFIRMED);
            pending.setConfirmedAt(LocalDateTime.now());
            repository.save(pending);
            invalidateHarnessCache(user.getId(), pending.getAction());

            result.put("success", execResult.isSuccess());
            result.put("message", execResult.getMessage());
            result.put("action", pending.getAction());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error executing action: " + e.getMessage());
        }

        return result;
    }

    @Transactional
    public Map<String, Object> confirmAndExecuteById(Long actionId, String email) {
        Map<String, Object> result = new HashMap<>();
        if (actionId == null) {
            result.put("success", false);
            result.put("message", "actionId is required");
            return result;
        }
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "User not found");
            return result;
        }
        User user = optUser.get();
        Optional<AiPendingAction> optPending = repository.findByIdAndUserId(actionId, user.getId());
        if (optPending.isEmpty()) {
            result.put("success", false);
            result.put("message", "Pending action not found");
            return result;
        }

        AiPendingAction pending = optPending.get();
        if (pending.getStatus() != AiPendingAction.Status.PENDING) {
            result.put("success", false);
            result.put("message", "Pending action is not in PENDING state");
            return result;
        }
        if (pending.getExpiresAt() != null && pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            pending.setStatus(AiPendingAction.Status.EXPIRED);
            repository.save(pending);
            result.put("success", false);
            result.put("message", "Action has expired");
            return result;
        }

        try {
            JsonNode args = objectMapper.readTree(pending.getPayload());
            AiToolExecutorService.ToolExecuteResult execResult =
                    aiToolExecutorService.execute(pending.getAction(), args, user);

            pending.setStatus(AiPendingAction.Status.CONFIRMED);
            pending.setConfirmedAt(LocalDateTime.now());
            repository.save(pending);
            invalidateHarnessCache(user.getId(), pending.getAction());

            result.put("success", execResult.isSuccess());
            result.put("message", execResult.getMessage());
            result.put("action", pending.getAction());
            result.put("actionId", pending.getId());
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error executing action: " + e.getMessage());
            return result;
        }
    }

    @Transactional
    public void cancelPendingAction(String conversationId, String email) {
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            return;
        }
        User user = optUser.get();

        Optional<AiPendingAction> optPending = getPendingAction(user.getId(), conversationId);
        if (optPending.isPresent()) {
            AiPendingAction pending = optPending.get();
            pending.setStatus(AiPendingAction.Status.CANCELLED);
            pending.setCancelledAt(LocalDateTime.now());
            repository.save(pending);
        }
    }

    @Transactional
    public Map<String, Object> cancelPendingActionById(Long actionId, String email) {
        Map<String, Object> result = new HashMap<>();
        if (actionId == null) {
            result.put("success", false);
            result.put("message", "actionId is required");
            return result;
        }
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "User not found");
            return result;
        }
        User user = optUser.get();
        Optional<AiPendingAction> optPending = repository.findByIdAndUserId(actionId, user.getId());
        if (optPending.isEmpty()) {
            result.put("success", false);
            result.put("message", "Pending action not found");
            return result;
        }
        AiPendingAction pending = optPending.get();
        if (pending.getStatus() != AiPendingAction.Status.PENDING) {
            result.put("success", false);
            result.put("message", "Pending action is not in PENDING state");
            return result;
        }

        pending.setStatus(AiPendingAction.Status.CANCELLED);
        pending.setCancelledAt(LocalDateTime.now());
        repository.save(pending);
        result.put("success", true);
        result.put("message", "Pending action cancelled");
        result.put("actionId", pending.getId());
        return result;
    }

    private void invalidateHarnessCache(Long userId, String action) {
        if (userId == null || action == null) {
            return;
        }
        if (!isMutatingAction(action)) {
            return;
        }
        semanticCacheService.invalidateAll(userId);
    }

    private boolean isMutatingAction(String action) {
        return switch (action) {
            case "enroll_course", "unenroll_course",
                 "create_course", "update_course", "publish_course", "delete_course",
                 "create_lesson", "update_lesson", "delete_lesson",
                 "create_exam", "update_exam", "publish_exam", "delete_exam",
                 "create_question", "bulk_import_questions" -> true;
            default -> false;
        };
    }
}
