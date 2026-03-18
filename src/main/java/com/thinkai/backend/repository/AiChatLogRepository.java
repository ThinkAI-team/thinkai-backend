package com.thinkai.backend.repository;

import com.thinkai.backend.entity.AiChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AiChatLogRepository extends JpaRepository<AiChatLog, Long> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
