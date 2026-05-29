package org.rap.algotutorbe.ai.repository;

import org.rap.algotutorbe.ai.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {
    List<AiMessage> findTop10ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    long countByConversationIdAndRoleAndMode(UUID conversationId, org.rap.algotutorbe.ai.enums.AiMessageRole role, String mode);

    @Query("SELECT COALESCE(SUM(m.tokenInput), 0) FROM AiMessage m")
    Long sumInputTokens();

    @Query("SELECT COALESCE(SUM(m.tokenOutput), 0) FROM AiMessage m")
    Long sumOutputTokens();

    @Query(value = "SELECT CAST(m.created_at AS date) as day, " +
                   "COALESCE(SUM(m.token_input), 0) as input_tokens, " +
                   "COALESCE(SUM(m.token_output), 0) as output_tokens " +
                   "FROM ai_messages m " +
                   "WHERE m.created_at >= :since " +
                   "GROUP BY CAST(m.created_at AS date) " +
                   "ORDER BY day ASC", nativeQuery = true)
    List<Object[]> getDailyTokenUsage(@Param("since") java.time.Instant since);

    @Query("SELECT m.mode, COALESCE(SUM(m.tokenInput), 0), COALESCE(SUM(m.tokenOutput), 0) " +
           "FROM AiMessage m GROUP BY m.mode")
    List<Object[]> getTokenUsageByMode();

    @Query(value = "SELECT CAST(u.id AS varchar), u.username, u.email, " +
                   "COALESCE(SUM(m.token_input), 0) as input_tokens, " +
                   "COALESCE(SUM(m.token_output), 0) as output_tokens " +
                   "FROM ai_messages m " +
                   "JOIN users u ON m.user_id = u.id " +
                   "GROUP BY u.id, u.username, u.email " +
                   "ORDER BY (COALESCE(SUM(m.token_input), 0) + COALESCE(SUM(m.token_output), 0)) DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopTokenConsumers(@Param("limit") int limit);
}

