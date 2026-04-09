package org.rap.algotutorbe.problem.domain.repositories;

import org.rap.algotutorbe.problem.domain.models.Editorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EditorialRepository extends JpaRepository<Editorial, Long> {
    void deleteAllByProblemId(Long id);
}
