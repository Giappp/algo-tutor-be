package org.rap.algotutorbe.quiz.domain.repositories;

import org.rap.algotutorbe.quiz.domain.models.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :id")
    Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.lesson.id = :lessonId")
    Optional<Quiz> findByLessonIdWithQuestions(@Param("lessonId") Long lessonId);

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.lesson.id = :lessonId")
    Optional<Quiz> findByLessonId(@Param("lessonId") Long lessonId);

    boolean existsByLessonId(Long lessonId);
}
