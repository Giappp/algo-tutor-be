package org.rap.algotutorbe.submission.internal.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.rap.algotutorbe.submission.SubmissionCreatedEvent;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Getter
@Service
@NoArgsConstructor
public class Submission extends AbstractAggregateRoot<Submission> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long problemId;
    @Column(columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    private ProgrammingLanguage language;

    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    private Double executionTimeMs;
    private Double memoryUsedKb;

    private LocalDateTime submittedAt;

    public static Submission create(Long userId, Long problemId, String sourceCode, ProgrammingLanguage language) {
        Submission submission = new Submission();
        submission.userId = userId;
        submission.problemId = problemId;
        submission.sourceCode = sourceCode;
        submission.language = language;
        submission.verdict = Verdict.PENDING;
        submission.submittedAt = LocalDateTime.now();

        submission.registerEvent(new SubmissionCreatedEvent(
                submission.id,
                submission.problemId,
                submission.sourceCode,
                submission.language.name()
        ));

        return submission;
    }

    public void markAsJudged(Verdict verdict) {
        this.verdict = verdict;
    }
}
