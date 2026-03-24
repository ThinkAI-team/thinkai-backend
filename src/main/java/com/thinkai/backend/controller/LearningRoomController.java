package com.thinkai.backend.controller;

import com.thinkai.backend.dto.CourseDetailResponse;
import com.thinkai.backend.dto.LessonDetailResponse;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.LearningRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class LearningRoomController {

    private final LearningRoomService learningRoomService;

    @StudentOnly
    @GetMapping("/{courseId}/learn")
    public ResponseEntity<Map<String, Object>> getCourseWithLessons(Authentication auth, @PathVariable Long courseId) {
        CourseDetailResponse response = learningRoomService.getCourseWithLessons(auth.getName(), courseId);
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", response));
    }

    @StudentOnly
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<Map<String, Object>> getLessonDetail(Authentication auth, @PathVariable Long lessonId) {
        LessonDetailResponse response = learningRoomService.getLessonDetail(auth.getName(), lessonId);
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", response));
    }
}
