package com.thinkai.backend.service;

import com.thinkai.backend.dto.AdminAiRuntimeSettingsDto;
import com.thinkai.backend.entity.AdminAuditLog;
import com.thinkai.backend.entity.AiSettings;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.AdminAuditLogRepository;
import com.thinkai.backend.repository.AiSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRuntimeSettingsService {

    private static final Long ADMIN_USER_ID = 1L;
    private static final String KEY_TUTOR_ENABLED = "admin.ai.runtime.tutorEnabled";
    private static final String KEY_HARNESS_ENABLED = "admin.ai.runtime.harnessEnabled";
    private static final String KEY_TUTOR_MODEL = "admin.ai.runtime.tutorModel";
    private static final String KEY_TUTOR_FALLBACK_MODEL = "admin.ai.runtime.tutorFallbackModel";
    private static final String KEY_HARNESS_MODELS = "admin.ai.runtime.harnessModels";
    private static final String KEY_BLOCKED_MODELS = "admin.ai.runtime.blockedModels";
    private static final String AUDIT_ACTION = "AI_RUNTIME_SETTINGS_UPDATE";

    private final AiSettingsRepository aiSettingsRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional(readOnly = true)
    public AdminAiRuntimeSettingsDto getSettings() {
        return AdminAiRuntimeSettingsDto.builder()
                .tutorEnabled(readBoolean(KEY_TUTOR_ENABLED, true))
                .harnessEnabled(readBoolean(KEY_HARNESS_ENABLED, true))
                .tutorModel(readText(KEY_TUTOR_MODEL))
                .tutorFallbackModel(readText(KEY_TUTOR_FALLBACK_MODEL))
                .harnessModels(readStringList(KEY_HARNESS_MODELS))
                .blockedModels(readStringList(KEY_BLOCKED_MODELS))
                .build();
    }

    @Transactional
    public AdminAiRuntimeSettingsDto updateSettings(AdminAiRuntimeSettingsDto dto) {
        return updateSettings(dto, "unknown-admin");
    }

    @Transactional
    public AdminAiRuntimeSettingsDto updateSettings(AdminAiRuntimeSettingsDto dto, String actor) {
        if (dto == null) {
            return getSettings();
        }

        AdminAiRuntimeSettingsDto before = getSettings();
        AdminAiRuntimeSettingsDto normalized = normalizeIncoming(dto);
        validateSettings(normalized);

        write(KEY_TUTOR_ENABLED, Boolean.toString(normalized.isTutorEnabled()));
        write(KEY_HARNESS_ENABLED, Boolean.toString(normalized.isHarnessEnabled()));
        write(KEY_TUTOR_MODEL, normalized.getTutorModel());
        write(KEY_TUTOR_FALLBACK_MODEL, normalized.getTutorFallbackModel());
        write(KEY_HARNESS_MODELS, joinStringList(normalized.getHarnessModels()));
        write(KEY_BLOCKED_MODELS, joinStringList(normalized.getBlockedModels()));

        AdminAiRuntimeSettingsDto after = getSettings();
        String safeActor = safeActor(actor);
        String diff = buildDiff(before, after);
        log.info("Admin AI runtime settings updated by {}: {}", safeActor, diff);
        saveAuditLog(safeActor, before, after, diff);
        return after;
    }

    public boolean isTutorEnabled() {
        return readBoolean(KEY_TUTOR_ENABLED, true);
    }

    public boolean isHarnessEnabled() {
        return readBoolean(KEY_HARNESS_ENABLED, true);
    }

    public boolean isModelBlocked(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        return matchesBlockedRule(model, readStringList(KEY_BLOCKED_MODELS));
    }

    private String readText(String key) {
        return aiSettingsRepository.findByUserIdAndSettingKey(ADMIN_USER_ID, key)
                .map(AiSettings::getSettingValue)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(null);
    }

    private boolean readBoolean(String key, boolean defaultValue) {
        String value = readText(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private List<String> readStringList(String key) {
        String value = readText(key);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,\\n\\r]+"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String joinStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .map(item -> item == null ? "" : item.trim())
                .filter(item -> !item.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AdminAiRuntimeSettingsDto normalizeIncoming(AdminAiRuntimeSettingsDto dto) {
        List<String> blocked = normalizeStringList(dto.getBlockedModels());
        List<String> harness = normalizeStringList(dto.getHarnessModels());
        return AdminAiRuntimeSettingsDto.builder()
                .tutorEnabled(dto.isTutorEnabled())
                .harnessEnabled(dto.isHarnessEnabled())
                .tutorModel(normalizeOptionalText(dto.getTutorModel()))
                .tutorFallbackModel(normalizeOptionalText(dto.getTutorFallbackModel()))
                .harnessModels(harness)
                .blockedModels(blocked)
                .build();
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::normalizeOptionalText)
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateSettings(AdminAiRuntimeSettingsDto dto) {
        if (dto.isTutorEnabled() && dto.getTutorModel() != null
                && matchesBlockedRule(dto.getTutorModel(), dto.getBlockedModels())) {
            boolean hasFallback = dto.getTutorFallbackModel() != null
                    && !matchesBlockedRule(dto.getTutorFallbackModel(), dto.getBlockedModels())
                    && !dto.getTutorFallbackModel().equalsIgnoreCase(dto.getTutorModel());
            if (!hasFallback) {
                throw new ApiException(
                        "Tutor model is blocked and no valid fallback model is available.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        if (dto.isHarnessEnabled() && !dto.getHarnessModels().isEmpty()) {
            boolean hasUsableHarnessModel = dto.getHarnessModels().stream()
                    .anyMatch(value -> !matchesBlockedRule(value, dto.getBlockedModels()));
            if (!hasUsableHarnessModel) {
                throw new ApiException(
                        "All configured harness models are blocked.",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    private boolean matchesBlockedRule(String model, List<String> blockedRules) {
        if (model == null || model.isBlank() || blockedRules == null || blockedRules.isEmpty()) {
            return false;
        }
        String normalizedModel = model.trim().toLowerCase(Locale.ROOT);
        String provider = normalizedModel.contains("/")
                ? normalizedModel.substring(0, normalizedModel.indexOf('/'))
                : normalizedModel;

        for (String rawRule : blockedRules) {
            if (rawRule == null || rawRule.isBlank()) {
                continue;
            }
            String rule = rawRule.trim().toLowerCase(Locale.ROOT);

            if ("*".equals(rule)) {
                return true;
            }
            if (rule.endsWith("*")) {
                String prefix = rule.substring(0, rule.length() - 1).trim();
                if (!prefix.isEmpty() && normalizedModel.startsWith(prefix)) {
                    return true;
                }
                continue;
            }

            if (rule.equals(normalizedModel)) {
                return true;
            }

            // Family/provider block (e.g. "qwen", "openai", "google")
            if (!rule.contains("/") && !rule.contains(":")) {
                if (provider.equals(rule) || normalizedModel.startsWith(rule + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String safeActor(String actor) {
        String normalized = normalizeOptionalText(actor);
        return normalized != null ? normalized : "unknown-admin";
    }

    private String buildDiff(AdminAiRuntimeSettingsDto before, AdminAiRuntimeSettingsDto after) {
        List<String> changes = new ArrayList<>();
        if (before.isTutorEnabled() != after.isTutorEnabled()) {
            changes.add("tutorEnabled: " + before.isTutorEnabled() + " -> " + after.isTutorEnabled());
        }
        if (before.isHarnessEnabled() != after.isHarnessEnabled()) {
            changes.add("harnessEnabled: " + before.isHarnessEnabled() + " -> " + after.isHarnessEnabled());
        }
        if (!Objects.equals(before.getTutorModel(), after.getTutorModel())) {
            changes.add("tutorModel: " + before.getTutorModel() + " -> " + after.getTutorModel());
        }
        if (!Objects.equals(before.getTutorFallbackModel(), after.getTutorFallbackModel())) {
            changes.add("tutorFallbackModel: " + before.getTutorFallbackModel() + " -> " + after.getTutorFallbackModel());
        }
        if (!Objects.equals(before.getHarnessModels(), after.getHarnessModels())) {
            changes.add("harnessModels: " + before.getHarnessModels() + " -> " + after.getHarnessModels());
        }
        if (!Objects.equals(before.getBlockedModels(), after.getBlockedModels())) {
            changes.add("blockedModels: " + before.getBlockedModels() + " -> " + after.getBlockedModels());
        }
        return changes.isEmpty() ? "no changes" : String.join("; ", changes);
    }

    private void saveAuditLog(String actor, AdminAiRuntimeSettingsDto before,
                              AdminAiRuntimeSettingsDto after, String diffSummary) {
        AdminAuditLog audit = AdminAuditLog.builder()
                .actor(actor)
                .action(AUDIT_ACTION)
                .resourceType("AI_RUNTIME_SETTINGS")
                .resourceKey("GLOBAL")
                .beforeSnapshot(toSnapshot(before))
                .afterSnapshot(toSnapshot(after))
                .diffSummary(diffSummary)
                .build();
        adminAuditLogRepository.save(audit);
    }

    private String toSnapshot(AdminAiRuntimeSettingsDto dto) {
        if (dto == null) {
            return "{}";
        }
        return "{"
                + "\"tutorEnabled\":" + dto.isTutorEnabled()
                + ",\"harnessEnabled\":" + dto.isHarnessEnabled()
                + ",\"tutorModel\":" + quote(dto.getTutorModel())
                + ",\"tutorFallbackModel\":" + quote(dto.getTutorFallbackModel())
                + ",\"harnessModels\":" + quoteList(dto.getHarnessModels())
                + ",\"blockedModels\":" + quoteList(dto.getBlockedModels())
                + "}";
    }

    private String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String quoteList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .map(this::quote)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private void write(String key, String value) {
        Optional<AiSettings> settingOpt = aiSettingsRepository.findByUserIdAndSettingKey(ADMIN_USER_ID, key);
        if (value == null || value.isBlank()) {
            settingOpt.ifPresent(aiSettingsRepository::delete);
            return;
        }

        AiSettings setting = settingOpt.orElseGet(() -> AiSettings.builder()
                .userId(ADMIN_USER_ID)
                .settingKey(key)
                .build());
        setting.setSettingValue(value);
        setting.setUpdatedBy(ADMIN_USER_ID);
        aiSettingsRepository.save(setting);
    }
}
