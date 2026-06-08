package org.rap.algotutorbe.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;

import java.util.UUID;

@Entity
@Table(name = "ai_conversation_state", indexes = {
    @Index(name = "idx_ai_conversation_state_conversation_id", columnList = "conversation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationState extends BaseUuidEntity {
    private UUID conversationId;

    private Long problemId;

    private Long lessonId;

    private String lessonSlug;

    private Integer hintLevel;

    private Boolean solutionUnlocked;

    private String currentIntent;

    private String lastCodeHash;

    private Long lastSubmissionId;

    @Column(columnDefinition = "TEXT")
    private String lastFailedTestCase;

    private String lastDiscussedConcept;
}
