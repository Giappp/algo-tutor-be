package org.rap.algotutorbe.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.ai.enums.ConversationType;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AIConversation extends BaseUuidEntity {
    @Column(nullable = false)
    private UUID userId;

    private Long lessonId;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LLMProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
