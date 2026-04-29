package org.rap.algotutorbe.learning.models;

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
    @JoinColumn(name = "coding_lesson_id", nullable = false)
    private CodingLesson codingLesson;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String stdin;

    @Column(name = "expected_stdout", columnDefinition = "TEXT", nullable = false)
    private String expectedStdout;

    @Column(name = "is_hidden")
    private boolean isHidden;

    @Column(name = "sort_order")
    private int orderIndex;

    private String explanation;
}
