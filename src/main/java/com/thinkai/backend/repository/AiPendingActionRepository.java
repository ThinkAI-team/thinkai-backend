package com.thinkai.backend.repository;

import com.thinkai.backend.entity.AiPendingAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiPendingActionRepository extends JpaRepository<AiPendingAction, Long> {

    List<AiPendingAction> findByUserIdAndConversationIdAndStatus(
            Long userId, String conversationId, AiPendingAction.Status status);

    Optional<AiPendingAction> findTopByUserIdAndConversationIdAndStatusOrderByCreatedAtDesc(
            Long userId, String conversationId, AiPendingAction.Status status);

    Optional<AiPendingAction> findByIdAndUserId(Long id, Long userId);

    void deleteByUserIdAndConversationIdAndStatus(
            Long userId, String conversationId, AiPendingAction.Status status);
}