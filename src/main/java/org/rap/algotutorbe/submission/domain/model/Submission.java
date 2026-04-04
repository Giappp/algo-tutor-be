package org.rap.algotutorbe.submission.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
public class Submission extends BaseEntity {
    private Long userId;

    private Long problemId;
    @Column(columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    private ProgrammingLanguage language;

    private Integer totalTestcases;
    private Integer passedTestcases = 0;

    private Double maxTime;

    private Integer maxMemory;

    @Column(columnDefinition = "TEXT")
    private String compileOutput;

    private Verdict verdict;

    public static Submission create(Long userId, Long problemId, String sourceCode, ProgrammingLanguage language) {
        Submission submission = new Submission();
        submission.userId = userId;
        submission.problemId = problemId;
        submission.sourceCode = sourceCode;
        submission.language = language;

        return submission;
    }
}
