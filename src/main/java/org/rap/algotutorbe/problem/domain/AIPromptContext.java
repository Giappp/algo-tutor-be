package org.rap.algotutorbe.problem.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.rap.algotutorbe.common.domain.BaseEntity;

@Entity
@Table(name = "ai_prompt_contexts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AIPromptContext extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Column(columnDefinition = "TEXT")
    private String algorithmicConcept;

    @Column(columnDefinition = "TEXT")
    private String predefinedHints;

    @Column(columnDefinition = "TEXT")
    private String edgeCasesToRemind;
}
