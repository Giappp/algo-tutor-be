package org.rap.algotutorbe.submission.repositories;

import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    @Query("""
            SELECT DISTINCT s FROM Submission s
            LEFT JOIN FETCH s.codingLesson
            LEFT JOIN FETCH s.submissionDetails d
            LEFT JOIN FETCH d.testcase
            WHERE s.id = :id
            """)
    Optional<Submission> findByIdWithLesson(@Param("id") UUID id);

    @Query("""
            SELECT s FROM Submission s
            JOIN s.codingLesson l
            WHERE s.user.id = :userId
              AND (:lessonSlug IS NULL OR l.slug = :lessonSlug)
              AND (:status IS NULL OR s.verdict = :status)
              AND (:language IS NULL OR s.language = :language)
            ORDER BY s.createdAt DESC
            """)
    Page<Submission> findMySubmissions(
            @Param("userId") UUID userId,
            @Param("lessonSlug") String lessonSlug,
            @Param("status") Verdict status,
            @Param("language") ProgrammingLanguage language,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM Submission s
            JOIN s.codingLesson l
            WHERE s.user.id = :userId
              AND l.slug = :lessonSlug
            ORDER BY s.createdAt DESC
            """)
    List<Submission> findMySubmissionsByLessonSlug(
            @Param("userId") UUID userId,
            @Param("lessonSlug") String lessonSlug
    );

    Optional<Submission> findTopByUserIdAndCodingLessonIdOrderByCreatedAtDesc(UUID userId, Long codingLessonId);

    @Query("SELECT s.createdAt FROM Submission s WHERE s.user.id = :userId AND s.createdAt >= :startDate AND s.createdAt < :endDate")
    List<Instant> findSubmissionDates(@Param("userId") UUID userId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT s.verdict, COUNT(s) FROM Submission s GROUP BY s.verdict")
    List<Object[]> getVerdictDistribution();
}
