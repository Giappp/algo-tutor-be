package org.rap.algotutorbe.problem.domain.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.enums.DifficultyConverter;
import org.rap.algotutorbe.submission.entities.Submission;

import java.util.*;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
public class Problem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String statement;

    @Convert(converter = DifficultyConverter.class)
    private Difficulty difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<ProblemExample> examples = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> constraints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "starter_code", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> starterCode = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> hints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_insights", columnDefinition = "jsonb", nullable = false)
    private List<String> keyInsights = new ArrayList<>();

    @Column(name = "base_time_limit_ms", nullable = false)
    private Integer baseTimeLimitMs = 1000;

    @Column(name = "base_memory_limit_mb", nullable = false)
    private Integer baseMemoryLimitMb = 256;

    @Setter(lombok.AccessLevel.NONE)
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Testcase> testCases = new ArrayList<>();

    @Setter(lombok.AccessLevel.NONE)
    @ManyToMany
    @JoinTable(
            name = "problem_tags",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Setter(lombok.AccessLevel.NONE)
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Editorial> editorials = new ArrayList<>();

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Submission> submissions = new ArrayList<>();

    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getProblems().add(this); // Nếu Tag cũng có mappedBy = "tags"
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getProblems().remove(this);
    }

    public void addTestCase(Testcase testCase) {
        testCases.add(testCase);
        testCase.setProblem(this);
    }

    public void removeTestCase(Testcase testCase) {
        testCases.remove(testCase);
        testCase.setProblem(null);
    }

    public void addEditorial(Editorial editorial) {
        editorials.add(editorial);
        editorial.setProblem(this);
    }

    public void removeEditorial(Editorial editorial) {
        editorials.remove(editorial);
        editorial.setProblem(null);
    }
}