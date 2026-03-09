package org.rap.algotutorbe.problem.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

@Entity
@Table(name = "editorials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Editorial extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Column(columnDefinition = "TEXT")
    private String content;           // Markdown

    private boolean isVisible;        // toggled after contest ends
}
