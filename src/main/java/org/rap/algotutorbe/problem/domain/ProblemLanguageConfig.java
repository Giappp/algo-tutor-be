package org.rap.algotutorbe.problem.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

@Entity
@Table(name = "problem_language_configs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemLanguageConfig extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgrammingLanguage language;

    @Embedded
    private Constraints constraints;

    @Column(name = "code_template", columnDefinition = "TEXT")
    private String codeTemplate;
}
