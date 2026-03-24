package org.rap.algotutorbe.problem.domain.repositories;

import org.rap.algotutorbe.problem.domain.models.ProblemLanguageConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemLangConfigRepository extends JpaRepository<ProblemLanguageConfig, Long> {
    void deleteAllByProblemId(Long id);
}
