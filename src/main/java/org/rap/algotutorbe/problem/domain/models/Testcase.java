package org.rap.algotutorbe.problem.domain.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Testcase extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String stdin;

    @Column(name = "expected_stdout", columnDefinition = "TEXT", nullable = false)
    private String expectedStdout;

    @Column(name = "is_hidden")
    private boolean hidden;

    @Column(name = "sort_order")
    private int orderIndex;

    private String explanation;

    @Transient
    public boolean isSample() {
        return !hidden;
    }
}
