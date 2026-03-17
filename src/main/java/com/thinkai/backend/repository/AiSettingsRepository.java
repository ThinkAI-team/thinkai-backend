package com.thinkai.backend.repository;

import com.thinkai.backend.entity.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiSettingsRepository extends JpaRepository<AiSettings, Long> {

    Optional<AiSettings> findBySettingKey(String settingKey);
}
