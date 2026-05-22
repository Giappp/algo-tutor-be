package org.rap.algotutorbe.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;

import java.util.UUID;

@Entity
@Table(name = "ai_conversation_state")
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
