package com.thinkai.backend.service;

import com.thinkai.backend.dto.NotificationResponse;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.Notification;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.NotificationRepository;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationRealtimeService notificationRealtimeService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Transactional
    public void notifyStudentsWhenCourseCreated(Long teacherId, Course course) {
        if (course == null || course.getId() == null) {
            return;
        }

        User teacher = teacherId != null
                ? userRepository.findById(teacherId).orElse(null)
                : null;
        String teacherName = teacher != null && teacher.getFullName() != null
                ? teacher.getFullName()
                : "Giảng viên";

        List<User> students = userRepository.findByRoleAndIsActiveTrue(User.Role.STUDENT);
        if (students.isEmpty()) {
            return;
        }

        String title = "Khóa học mới từ " + teacherName;
        String message = course.getTitle() != null ? course.getTitle() : "Khóa học mới";
        String payloadJson = toPayloadJson(Map.of("courseId", course.getId(), "teacherId", teacherId));

        List<Notification> notifications = students.stream()
                .map(student -> Notification.builder()
                        .userId(student.getId())
                        .type("NEW_COURSE")
                        .title(title)
                        .message(message)
                        .payloadJson(payloadJson)
                        .isRead(false)
                        .build())
                .toList();

        List<Notification> saved = notificationRepository.saveAll(notifications);
        saved.forEach(item -> notificationRealtimeService.pushToUser(item.getUserId(), toResponse(item)));
        log.info("Pushed {} NEW_COURSE notifications for courseId={}", saved.size(), course.getId());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), safeSize);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        Map<String, Object> map = new HashMap<>();
        map.put("unreadCount", count);
        return map;
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND));
        if (!notification.getUserId().equals(userId)) {
            throw new ApiException("Bạn không có quyền với thông báo này", HttpStatus.FORBIDDEN);
        }
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public Map<String, Object> markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        int updated = 0;
        for (Notification item : notifications) {
            item.setIsRead(true);
            item.setReadAt(LocalDateTime.now());
            updated++;
        }
        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
        return Map.of("updated", updated);
    }

    @Transactional(readOnly = true)
    public User resolveUserFromStreamToken(String token) {
        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            throw new ApiException("Token không hợp lệ", HttpStatus.UNAUTHORIZED);
        }
        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private NotificationResponse toResponse(Notification item) {
        return NotificationResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .message(item.getMessage())
                .payload(parsePayload(item.getPayloadJson()))
                .isRead(item.getIsRead())
                .createdAt(item.getCreatedAt())
                .readAt(item.getReadAt())
                .build();
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String toPayloadJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
