package com.thinkai.backend.controller;

import com.thinkai.backend.dto.admin.AiPromptsRequest;
import com.thinkai.backend.dto.admin.AiPromptsResponse;
import com.thinkai.backend.dto.common.ApiResponse;
import com.thinkai.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * GET /admin/settings/ai-prompts
     * Lấy cấu hình AI prompts hiện tại
     */
    @GetMapping("/settings/ai-prompts")
    public ResponseEntity<ApiResponse<AiPromptsResponse>> getAiPrompts() {
        AiPromptsResponse data = adminService.getCurrentPrompts();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * PUT /admin/settings/ai-prompts
     * Cập nhật AI system prompts (TUTOR_SYSTEM_PROMPT + EXAM_GENERATOR_PROMPT)
     */
    @PutMapping("/settings/ai-prompts")
    public ResponseEntity<ApiResponse<AiPromptsResponse>> updateAiPrompts(
            @Valid @RequestBody AiPromptsRequest request) {
        AiPromptsResponse data = adminService.updateAiPrompts(request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật AI prompts thành công", data));
    }
}
