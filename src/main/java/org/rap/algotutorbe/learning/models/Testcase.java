package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

@Entity
@Table(name = "test_cases", indexes = {
    @Index(name = "idx_test_cases_coding_lesson_id", columnList = "coding_lesson_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Testcase extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coding_lesson_id", nullable = false)
    private CodingLesson codingLesson;

    @Column(nullable = false)
    private String inputFileUrl;

    @Column(nullable = false)
    private String outputFileUrl;

    @Column(nullable = false)
    private String inputFileKey;

    @Column(nullable = false)
    private String outputFileKey;

    private Integer scoreWeight;

    @Column(name = "isSample")
    private Boolean isSample;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
