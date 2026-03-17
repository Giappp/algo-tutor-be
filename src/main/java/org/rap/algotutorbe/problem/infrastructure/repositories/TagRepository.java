package org.rap.algotutorbe.problem.infrastructure.repositories;

import org.rap.algotutorbe.problem.domain.ProblemTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<ProblemTag, Long> {
}
