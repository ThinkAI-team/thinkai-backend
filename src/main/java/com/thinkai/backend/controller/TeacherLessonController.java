package com.thinkai.backend.controller;

import com.thinkai.backend.dto.LessonOrderRequest;
import com.thinkai.backend.dto.LessonRequest;
import com.thinkai.backend.entity.Lesson;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherLessonController {

    private final LessonService lessonService;
    private final UserRepository userRepository;

    private Long getTeacherId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .map(User::getId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    @PostMapping("/courses/{courseId}/lessons")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Lesson> createLesson(
            Authentication auth,
            @PathVariable Long courseId,
            @Valid @RequestBody LessonRequest request) {
        Lesson lesson = lessonService.createLesson(courseId, getTeacherId(auth), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(lesson);
    }

    @PostMapping("/courses/{courseId}/lessons/upload")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadLessonFile(
            Authentication auth,
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {
        String fileUrl = lessonService.uploadLessonFile(courseId, getTeacherId(auth), file);
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }

    @PutMapping("/courses/{courseId}/lessons/order")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reorderLessons(
            Authentication auth,
            @PathVariable Long courseId,
            @Valid @RequestBody LessonOrderRequest request) {
        lessonService.reorderLessons(courseId, getTeacherId(auth), request);
        return ResponseEntity.ok(Map.of("message", "Lessons reordered successfully"));
    }
}
