package org.rap.algotutorbe.submission.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.CodingLesson;

import java.util.List;

@Entity
@Table(name = "submissions", indexes = {
    @Index(name = "idx_submissions_user_id", columnList = "user_id"),
    @Index(name = "idx_submissions_coding_lesson_id", columnList = "coding_lesson_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Submission extends BaseUuidEntity {
    @Version
    private Long version;
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionDetail> submissionDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coding_lesson_id", nullable = false)
    private CodingLesson codingLesson;

    @Column(name = "code", columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    private ProgrammingLanguage language;

    @Column(name = "total_test_cases")
    private Integer totalTestcases;

    @Column(name = "passed_test_cases")
    private Integer passedTestcases = 0;

    @Column(name = "execution_time_ms")
    private Integer executionTime;

    @Column(name = "memory_used_kb")
    private Integer memoryUsed;

    @Column(columnDefinition = "TEXT")
    private String compileOutput;

    @Column(name = "status")
    @Convert(converter = VerdictConverter.class)
    private Verdict verdict;

    @Column(name = "progress_updated", nullable = false)
    private Boolean progressUpdated = false;
}
