package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.enums.Level;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    boolean existsBySlug(String slug);

    Optional<LearningPath> findBySlug(String slug);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.slug = :slug AND lp.isPublished = true")
    Optional<LearningPath> findBySlugAndNotDeleted(@Param("slug") String slug);

    @EntityGraph(attributePaths = {"topics", "topics.lessons"})
    @Query("SELECT lp FROM LearningPath lp WHERE lp.id = :id")
    Optional<LearningPath> findByIdWithTopicsAndLessons(@Param("id") Long id);

    @EntityGraph(attributePaths = {"topics", "topics.lessons"})
    @Query("SELECT lp FROM LearningPath lp WHERE lp.slug = :slug AND lp.isPublished = true")
    Optional<LearningPath> findBySlugWithTopicsAndLessons(@Param("slug") String slug);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.isPublished = true AND (:level IS NULL OR lp.level = :level)")
    Page<LearningPath> findPublishedByLevel(@Param("level") Level level, Pageable pageable);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.level = :level")
    Page<LearningPath> findByDeletedFalseAndLevel(@Param("level") Level level, Pageable pageable);

    @Query("SELECT lp FROM LearningPath lp WHERE LOWER(lp.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<LearningPath> findByDeletedFalseAndSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.level = :level AND LOWER(lp.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<LearningPath> findByDeletedFalseAndLevelAndSearch(@Param("level") Level level, @Param("search") String search, Pageable pageable);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.isPublished = true")
    List<LearningPath> findByDeletedFalseAndIsPublishedTrue();

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.learningPath.id = :learningPathId AND e.status = 'ACTIVE'")
    int countActiveEnrollments(@Param("learningPathId") Long learningPathId);
}
