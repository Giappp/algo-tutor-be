package org.rap.algotutorbe.problem.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "problem_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemTag extends BaseEntity {
    private String name;
    @ManyToMany(mappedBy = "tags")
    private Set<Problem> problems = new HashSet<>();
}
