package com.thinkai.backend.controller;

import com.thinkai.backend.dto.UpdateProgressRequest;
import com.thinkai.backend.dto.UpdateProgressResponse;
import com.thinkai.backend.dto.VideoPreferenceDto;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.VideoProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class VideoController {

    private final VideoProgressService videoProgressService;

    /**
     * PUT /courses/lessons/{lessonId}/progress — Lưu tiến độ xem giữa chừng.
     * Frontend gọi mỗi 10-15s khi student đang xem video.
     */
    @StudentOnly
    @PutMapping("/courses/lessons/{lessonId}/progress")
    public ResponseEntity<Map<String, Object>> updateProgress(
            @PathVariable Long lessonId,
            Authentication auth,
            @Valid @RequestBody UpdateProgressRequest request) {

        UpdateProgressResponse response = videoProgressService
                .updateProgress(lessonId, auth.getName(), request);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Progress saved",
                "data", response));
    }

    /**
     * GET /users/me/preferences — Lấy cài đặt player.
     */
    @GetMapping("/users/me/preferences")
    public ResponseEntity<Map<String, Object>> getPreferences(Authentication auth) {
        VideoPreferenceDto pref = videoProgressService.getPreferences(auth.getName());

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Success",
                "data", pref));
    }

    /**
     * PUT /users/me/preferences — Cập nhật cài đặt player.
     */
    @PutMapping("/users/me/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            Authentication auth,
            @Valid @RequestBody VideoPreferenceDto dto) {

        VideoPreferenceDto pref = videoProgressService.savePreferences(auth.getName(), dto);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Preferences updated",
                "data", pref));
    }
}
