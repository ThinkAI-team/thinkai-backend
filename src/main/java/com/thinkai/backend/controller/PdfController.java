package com.thinkai.backend.controller;

import com.thinkai.backend.dto.PdfLessonResponse;
import com.thinkai.backend.dto.PdfProgressRequest;
import com.thinkai.backend.dto.PdfProgressResponse;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/courses/lessons")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    /**
     * GET /courses/lessons/{lessonId}/pdf
     * Lấy metadata + URL bài học PDF.
     */
    @GetMapping("/{lessonId}/pdf")
    @StudentOnly
    public ResponseEntity<Map<String, Object>> getPdfLesson(
            @PathVariable Long lessonId,
            Authentication auth) {

        PdfLessonResponse data = pdfService.getPdfLesson(lessonId, auth.getName());
        return ResponseEntity.ok(Map.of("status", 200, "data", data));
    }

    /**
     * POST /courses/lessons/{lessonId}/pdf/progress
     * Cập nhật tiến độ đọc PDF (trang hiện tại).
     */
    @PostMapping("/{lessonId}/pdf/progress")
    @StudentOnly
    public ResponseEntity<Map<String, Object>> updateReadingProgress(
            @PathVariable Long lessonId,
            @Valid @RequestBody PdfProgressRequest request,
            Authentication auth) {

        PdfProgressResponse data = pdfService.updateReadingProgress(lessonId, auth.getName(), request);
        return ResponseEntity.ok(Map.of("status", 200, "data", data));
    }

    /**
     * GET /courses/lessons/{lessonId}/pdf/progress
     * Khôi phục vị trí đọc cuối cùng khi quay lại bài.
     */
    @GetMapping("/{lessonId}/pdf/progress")
    @StudentOnly
    public ResponseEntity<Map<String, Object>> getReadingProgress(
            @PathVariable Long lessonId,
            Authentication auth) {

        PdfProgressResponse data = pdfService.getReadingProgress(lessonId, auth.getName());
        return ResponseEntity.ok(Map.of("status", 200, "data", data));
    }
}
