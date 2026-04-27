package org.rap.algotutorbe.quiz.domain.repositories;

import org.rap.algotutorbe.quiz.domain.models.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.id = :id")
    Optional<QuizAttempt> findByIdWithAnswers(@Param("id") UUID id);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findByUserIdAndQuizIdOrderByStartedAtDesc(@Param("userId") UUID userId, @Param("quizId") Long quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId AND qa.completedAt IS NULL")
    Optional<QuizAttempt> findIncompleteAttempt(@Param("userId") UUID userId, @Param("quizId") Long quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId AND qa.isPassed = true ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findPassedAttempts(@Param("userId") UUID userId, @Param("quizId") Long quizId);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.quiz.id = :quizId AND qa.isPassed = true")
    long countPassedAttempts(@Param("userId") UUID userId, @Param("quizId") Long quizId);
}
