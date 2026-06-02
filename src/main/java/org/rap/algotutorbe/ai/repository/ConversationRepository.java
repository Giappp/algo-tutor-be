package org.rap.algotutorbe.ai.repository;

import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.enums.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<AIConversation, UUID> {
    Optional<AIConversation> findByIdAndUserIdAndType(UUID id, UUID userId, ConversationType type);

    Optional<AIConversation> findTopByUserIdAndLessonIdAndTypeOrderByUpdatedAtDesc(UUID userId, Long id, ConversationType type);
}
