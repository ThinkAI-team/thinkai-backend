package com.thinkai.backend.repository;

import com.thinkai.backend.entity.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiSettingsRepository extends JpaRepository<AiSettings, Long> {

    List<AiSettings> findByUserId(Long userId);

    Optional<AiSettings> findByUserIdAndSettingKey(Long userId, String settingKey);
}
