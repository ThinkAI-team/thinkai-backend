package com.thinkai.backend.repository;

import com.thinkai.backend.entity.AiChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiChatLogRepository extends JpaRepository<AiChatLog, Long> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<AiChatLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AiChatLog> findByIdAndUserId(Long id, Long userId);

    List<AiChatLog> findByUserIdAndConversationIdOrderByCreatedAtAsc(Long userId, String conversationId);

    List<AiChatLog> findByUserIdAndConversationIdOrderByCreatedAtDesc(Long userId, String conversationId);

    // New methods for filtering by source
    List<AiChatLog> findByUserIdAndSourceOrderByCreatedAtDesc(Long userId, String source);

    List<AiChatLog> findByUserIdAndSourceAndConversationIdOrderByCreatedAtAsc(Long userId, String source, String conversationId);

    List<AiChatLog> findDistinctByUserIdAndSourceOrderByCreatedAtDesc(Long userId, String source);

    long countByUserIdAndSource(Long userId, String source);
}
