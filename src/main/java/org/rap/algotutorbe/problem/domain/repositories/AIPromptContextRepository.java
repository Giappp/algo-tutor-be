package org.rap.algotutorbe.problem.domain.repositories;

import org.rap.algotutorbe.problem.domain.models.AIPromptContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIPromptContextRepository extends JpaRepository<AIPromptContext, Long> {
}
