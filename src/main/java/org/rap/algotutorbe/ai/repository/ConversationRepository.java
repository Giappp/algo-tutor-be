package org.rap.algotutorbe.ai.repository;

import org.rap.algotutorbe.ai.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<AIConversation, UUID> {
    Optional<AIConversation> findByIdAndUserId(UUID id, UUID userId);

    Optional<AIConversation> findTopByUserIdAndLessonIdOrderByUpdatedAtDesc(UUID userId, Long id);
}
