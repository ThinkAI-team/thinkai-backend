package com.thinkai.backend.service;

import com.thinkai.backend.dto.admin.AiPromptsRequest;
import com.thinkai.backend.dto.admin.AiPromptsResponse;
import com.thinkai.backend.entity.AiSettings;
import com.thinkai.backend.repository.AiSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {

    // Key constants ─ khớp với giá trị lưu trong bảng ai_settings
    private static final String KEY_TUTOR_PROMPT = "TUTOR_SYSTEM_PROMPT";
    private static final String KEY_EXAM_PROMPT = "EXAM_GENERATOR_PROMPT";

    private final AiSettingsRepository aiSettingsRepository;

    /**
     * PUT /admin/settings/ai-prompts
     * Upsert 2 settings: TUTOR_SYSTEM_PROMPT và EXAM_GENERATOR_PROMPT
     */
    @Transactional
    public AiPromptsResponse updateAiPrompts(AiPromptsRequest request) {

        // Lấy userId của admin đang thực hiện (để ghi updatedBy)
        Long adminId = getCurrentUserId();

        upsertSetting(KEY_TUTOR_PROMPT, request.getTutorSystemPrompt(), adminId);
        upsertSetting(KEY_EXAM_PROMPT, request.getExamGeneratorPrompt(), adminId);

        return AiPromptsResponse.builder()
                .tutorSystemPrompt(request.getTutorSystemPrompt())
                .examGeneratorPrompt(request.getExamGeneratorPrompt())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Lấy setting hiện tại theo key (để GET xem config)
     */
    @Transactional(readOnly = true)
    public AiPromptsResponse getCurrentPrompts() {
        String tutorPrompt = aiSettingsRepository.findBySettingKey(KEY_TUTOR_PROMPT)
                .map(AiSettings::getSettingValue)
                .orElse("");

        String examPrompt = aiSettingsRepository.findBySettingKey(KEY_EXAM_PROMPT)
                .map(AiSettings::getSettingValue)
                .orElse("");

        return AiPromptsResponse.builder()
                .tutorSystemPrompt(tutorPrompt)
                .examGeneratorPrompt(examPrompt)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Tạo mới nếu chưa có, cập nhật nếu đã có (upsert) */
    private void upsertSetting(String key, String value, Long adminId) {
        AiSettings setting = aiSettingsRepository.findBySettingKey(key)
                .orElse(AiSettings.builder()
                        .settingKey(key)
                        .description("Auto-created by admin")
                        .build());

        setting.setSettingValue(value);
        setting.setUpdatedBy(adminId);
        aiSettingsRepository.save(setting);
    }

    /** Lấy ID user đang đăng nhập từ Security Context */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null
                    && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                // UserDetails.username = email → lookup nếu cần; ở đây trả null để đơn giản
            }
        } catch (Exception ignored) {
        }
        return null; // Có thể inject UserRepository để lookup id sau
    }
}
