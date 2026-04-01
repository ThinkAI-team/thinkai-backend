package com.thinkai.backend.controller;

import com.thinkai.backend.dto.LessonCompleteRequest;
import com.thinkai.backend.dto.LessonCompleteResponse;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.LessonProgressService;
import com.thinkai.backend.service.VideoProgressService;
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
        private final VideoProgressService videoProgressService;

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

        @StudentOnly
        @PutMapping("/{lessonId}/pdf-opened")
        public ResponseEntity<Map<String, Object>> markPdfOpened(
                        Authentication auth,
                        @PathVariable Long lessonId) {

                var response = videoProgressService.markPdfCompleted(lessonId, auth.getName());

                return ResponseEntity.ok(Map.of(
                                "status", 200,
                                "message", "PDF marked as completed",
                                "data", response));
        }
}
