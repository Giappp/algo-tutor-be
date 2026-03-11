package org.rap.algotutorbe.problem.infrastructure.repositories;

import org.rap.algotutorbe.problem.domain.ProblemTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<ProblemTag, Long> {
}
