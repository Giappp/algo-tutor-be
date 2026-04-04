package org.rap.algotutorbe.submission.domain.model;

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

    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    private Double time;
    private Integer memory;

    @Column(columnDefinition = "TEXT")
    private String compileOutput;
}
