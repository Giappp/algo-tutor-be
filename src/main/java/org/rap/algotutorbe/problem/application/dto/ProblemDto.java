package org.rap.algotutorbe.problem.application.dto;

import lombok.Getter;
import lombok.Setter;
import org.rap.algotutorbe.problem.domain.Problem;

@Getter
@Setter
public class ProblemDto {
    private Long id;
    private String slug;
    private String title;
    private String statement;
    private String difficulty;
    private String status;

    public ProblemDto(Problem problem) {
        this.id = problem.getId();
        this.slug = problem.getSlug();
        this.title = problem.getTitle();
        this.statement = problem.getStatement();
        this.difficulty = problem.getDifficulty().name();
        this.status = problem.getStatus().name();
    }
}
