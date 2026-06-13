package org.rap.algotutorbe.submission.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.learning.models.Testcase;

@Entity
@Table(name = "submission_detail", indexes = {
    @Index(name = "idx_submission_detail_submission_id", columnList = "submission_id"),
    @Index(name = "idx_submission_detail_testcase_id", columnList = "testcase_id")
})
@Getter
@Setter
public class SubmissionDetail extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;
    @ManyToOne(fetch = FetchType.LAZY)
    private Testcase testcase;

    @Convert(converter = VerdictConverter.class)
    private Verdict verdict;

    private Integer time;

    private Integer memory;

    @Column(columnDefinition = "TEXT")
    private String stdout;

    @Column(columnDefinition = "TEXT")
    private String stderr;
}
