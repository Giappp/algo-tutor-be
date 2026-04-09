package org.rap.algotutorbe.problem.domain.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

@Entity
@Table(name = "editorials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Editorial extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProgrammingLanguage language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Editorial)) return false;
        return id != null && id.equals(((Editorial) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
