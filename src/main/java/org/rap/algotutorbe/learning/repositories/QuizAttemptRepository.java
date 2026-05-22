package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.models.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {
    @Query("SELECT COALESCE(MAX(qa.attemptNumber), 0) + 1 FROM QuizAttempt qa where qa.quiz.id = :quizId")
    int getNextAttemptNumber(@Param("quizId") Long quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user = :user AND qa.lessonSlug = :lessonSlug")
    List<QuizAttempt> getQuizAttemptByUserAndLessonSlug(@Param("user") User user, @Param("lessonSlug") String lessonSlug);
}
