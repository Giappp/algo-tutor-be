package org.rap.algotutorbe.problem.infrastructure.repositories;

import lombok.NonNull;
import org.rap.algotutorbe.problem.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemRepository extends JpaRepository<@NonNull Problem, @NonNull Long> {
}
