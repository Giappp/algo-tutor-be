package org.rap.algotutorbe.problem.domain.repositories;

import org.rap.algotutorbe.problem.domain.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
}
