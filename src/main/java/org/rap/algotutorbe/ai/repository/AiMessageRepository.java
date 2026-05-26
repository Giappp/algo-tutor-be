package org.rap.algotutorbe.ai.repository;

import org.rap.algotutorbe.ai.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {
    List<AiMessage> findTop10ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    long countByConversationIdAndRoleAndMode(UUID conversationId, org.rap.algotutorbe.ai.enums.AiMessageRole role, String mode);
}

