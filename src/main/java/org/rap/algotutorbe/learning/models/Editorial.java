package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;

@Entity
@Table(name = "editorials", indexes = {
    @Index(name = "idx_editorials_coding_lesson_id", columnList = "coding_lesson_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Editorial extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coding_lesson_id", nullable = false)
    private CodingLesson codingLesson;

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
