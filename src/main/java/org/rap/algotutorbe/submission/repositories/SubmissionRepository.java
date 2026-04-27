package org.rap.algotutorbe.submission.repositories;

import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    @Query("SELECT s FROM Submission s LEFT JOIN FETCH s.problem WHERE s.id = :id")
    Optional<Submission> findByIdWithProblem(@Param("id") UUID id);

    @Query("""
            SELECT s FROM Submission s
            JOIN s.problem p
            WHERE s.user.id = :userId
              AND (:status IS NULL OR s.verdict = :status)
              AND (:language IS NULL OR s.language = :language)
            ORDER BY s.createdAt DESC
            """)
    Page<Submission> findMySubmissions(
            @Param("userId") UUID userId,
            @Param("problemSlug") String problemSlug,
            @Param("status") Verdict status,
            @Param("language") org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage language,
            Pageable pageable
    );
}
