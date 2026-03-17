package org.rap.algotutorbe.problem.infrastructure.repositories;

import lombok.NonNull;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<@NonNull Problem, @NonNull Long> {

    @Query("SELECT p FROM Problem p LEFT JOIN FETCH p.languageConfigs WHERE p.id = :id")
    Optional<Problem> findByIdWithConfigs(@Param("id") Long id);

    @Query("SELECT p FROM Problem p LEFT JOIN FETCH p.testCases WHERE p.id = :id")
    Optional<Problem> findByIdWithTestCases(@Param("id") Long id);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT p FROM Problem p
            LEFT JOIN FETCH p.tags
            WHERE p.status <> 'DELETED'
            """)
    Page<Problem> findAllForAdmin(Pageable pageable);

    Optional<Problem> findBySlug(String slug);

    @Query("""
            SELECT p FROM Problem p
            LEFT JOIN FETCH p.tags
            WHERE p.isBenchmarked = true
              AND p.status = :status
            """)
    Page<Problem> findAllPublished(@Param("status") ProblemStatus status, Pageable pageable);

    @Query("""
            SELECT p FROM Problem p
            LEFT JOIN FETCH p.languageConfigs lc
            WHERE p.slug = :slug
              AND p.isBenchmarked = true
              AND p.status = 'ACTIVE'
            """)
    Optional<Problem> findPublishedBySlug(@Param("slug") String slug);
}
