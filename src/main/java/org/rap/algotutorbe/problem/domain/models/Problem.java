package org.rap.algotutorbe.problem.domain.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;

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

    private Long authorId;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    private ProblemStatus status;

    @Column(name = "base_time_limit_ms", nullable = false)
    private Integer baseTimeLimitMs = 1000; // Mặc định 1 giây

    @Column(name = "base_memory_limit_mb", nullable = false)
    private Integer baseMemoryLimitMb = 256; // Mặc định 256 MB

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Testcase> testCases = new ArrayList<>();

    @OneToOne(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private AIPromptContext aiPromptContext;

    @ManyToMany
    @JoinTable(name = "problem_tags")
    private Set<Tag> tags = new HashSet<>();
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Editorial> editorials = new ArrayList<>();


    public void addTag(Tag tag) {
        this.tags.add(tag);
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
