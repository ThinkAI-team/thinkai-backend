package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.NotificationResponse;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.NotificationRealtimeService;
import com.thinkai.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRealtimeService notificationRealtimeService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = requireUserId(authentication);
        Page<NotificationResponse> result = notificationService.getMyNotifications(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Notifications", result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success("Unread count", notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            Authentication authentication,
            @PathVariable("id") Long notificationId) {
        Long userId = requireUserId(authentication);
        NotificationResponse updated = notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", updated));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead(Authentication authentication) {
        Long userId = requireUserId(authentication);
        Map<String, Object> result = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", result));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam("token") String token) {
        User user = notificationService.resolveUserFromStreamToken(token);
        return notificationRealtimeService.register(user.getId());
    }

    private Long requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ApiException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }
}
