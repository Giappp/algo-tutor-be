package org.rap.algotutorbe.problem.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Problem extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String statement;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    private ProblemStatus status;

    @Column(name = "model_solution_code", columnDefinition = "TEXT")
    private String modelSolutionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_solution_language")
    private ProgrammingLanguage modelSolutionLanguage;

    @Column(name = "is_benchmarked", nullable = false)
    private boolean isBenchmarked = false;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProblemLanguageConfig> languageConfigs = new ArrayList<>();

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Testcase> testCases = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "problem_tags")
    private Set<ProblemTag> tags = new HashSet<>();

    private Long authorId;

    public void addLanguageConfig(ProblemLanguageConfig config) {
        this.languageConfigs.add(config);
        config.setProblem(this);
    }

    public void markAsBenchmarked() {
        this.isBenchmarked = true;
    }
}
