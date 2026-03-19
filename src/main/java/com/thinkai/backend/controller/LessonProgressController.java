package com.thinkai.backend.controller;

import com.thinkai.backend.dto.LessonCompleteRequest;
import com.thinkai.backend.dto.LessonCompleteResponse;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/courses/lessons")
@RequiredArgsConstructor
public class LessonProgressController {

        private final LessonProgressService lessonProgressService;

        @StudentOnly
        @PostMapping("/{lessonId}/complete")
        public ResponseEntity<Map<String, Object>> completeLesson(
                        Authentication auth,
                        @PathVariable Long lessonId,
                        @RequestBody(required = false) LessonCompleteRequest request) {

                if (request == null) {
                        request = new LessonCompleteRequest();
                }

                LessonCompleteResponse response = lessonProgressService.completeLesson(
                                auth.getName(), lessonId, request);

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "Progress updated",
                                "data", response));
        }
}
