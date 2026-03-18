package com.thinkai.backend.service;

import com.thinkai.backend.dto.AiSettingsDto;
import com.thinkai.backend.entity.AiSettings;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.AiSettingsRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiSettingsService {

    private final AiSettingsRepository aiSettingsRepository;
    private final UserRepository userRepository;

    public AiSettingsDto getSettings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        List<AiSettings> settings = aiSettingsRepository.findByUserId(user.getId());
        
        Map<String, String> settingsMap = settings.stream()
                .collect(Collectors.toMap(AiSettings::getSettingKey, AiSettings::getSettingValue));

        return AiSettingsDto.builder()
                .language(settingsMap.getOrDefault("language", "English"))
                .responseLength(settingsMap.getOrDefault("responseLength", "detailed"))
                .build();
    }

    @Transactional
    public AiSettingsDto updateSettings(String email, AiSettingsDto dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        updateOrInsertSetting(user.getId(), "language", dto.getLanguage(), user.getId());
        updateOrInsertSetting(user.getId(), "responseLength", dto.getResponseLength(), user.getId());

        return getSettings(email);
    }

    private void updateOrInsertSetting(Long userId, String key, String value, Long updatedBy) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        
        AiSettings setting = aiSettingsRepository.findByUserIdAndSettingKey(userId, key)
                .orElseGet(() -> AiSettings.builder()
                        .userId(userId)
                        .settingKey(key)
                        .build());
                        
        setting.setSettingValue(value);
        setting.setUpdatedBy(updatedBy);
        
        aiSettingsRepository.save(setting);
    }
}
