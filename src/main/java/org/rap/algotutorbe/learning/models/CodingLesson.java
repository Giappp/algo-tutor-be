package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.rap.algotutorbe.submission.entities.Submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@Setter
public class CodingLesson extends Lesson {
    @Column(name = "statement", columnDefinition = "TEXT", nullable = false)
    private String statement;
    @Column(name = "base_time_limit_ms", nullable = false)
    private Integer baseTimeLimitMs = 2000;

    @Column(name = "base_memory_limit_mb", nullable = false)
    private Integer baseMemoryLimitMb = 256;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "starter_code", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> starterCode = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> constraints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> hints = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<ProblemExample> examples = new ArrayList<>();

    @OneToMany(mappedBy = "codingLesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Testcase> testCases = new ArrayList<>();

    @OneToMany(mappedBy = "codingLesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Editorial> editorials = new ArrayList<>();

    @OneToMany(mappedBy = "codingLesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Submission> submissions = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_insights", columnDefinition = "jsonb")
    private List<String> keyInsights = new ArrayList<>();

    public void addTestCase(Testcase testCase) {
        testCases.add(testCase);
        testCase.setCodingLesson(this);
    }

    public void removeTestCase(Testcase testCase) {
        testCases.remove(testCase);
        testCase.setCodingLesson(null);
    }
}
