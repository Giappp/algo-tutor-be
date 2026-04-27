package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPathRepository extends
        JpaRepository<LearningPath, Long>,
        JpaSpecificationExecutor<LearningPath> {

    @Query("SELECT lp FROM LearningPath lp WHERE lp.deleted = false")
    List<LearningPath> findAllActive();

    @Query("SELECT lp FROM LearningPath lp WHERE lp.deleted = false AND lp.level = :level")
    List<LearningPath> findAllActiveByLevel(@Param("level") Level level);

    boolean existsBySlug(String slug);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.deleted = false AND lp.slug = :slug")
    Optional<LearningPath> findBySlug(@Param("slug") String slug);
}
