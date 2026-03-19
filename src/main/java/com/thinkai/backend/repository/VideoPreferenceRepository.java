package com.thinkai.backend.repository;

import com.thinkai.backend.entity.VideoPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoPreferenceRepository extends JpaRepository<VideoPreference, Long> {

    Optional<VideoPreference> findByUserId(Long userId);
}
