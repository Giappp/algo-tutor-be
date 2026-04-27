package org.rap.algotutorbe.submission.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

@Entity
@Table(name = "submission_testcases")
@Getter
@Setter
public class SubmissionTestcase extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    private Integer testcaseIndex;

    @Convert(converter = VerdictConverter.class)
    private Verdict verdict;

    private Double time;
    private Integer memory;

    @Column(columnDefinition = "TEXT")
    private String stdout;

    @Column(columnDefinition = "TEXT")
    private String compileOutput;
}
