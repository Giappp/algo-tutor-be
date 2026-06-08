package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuizIdOrderByOrderIndex(Long quizId);

    @Query("SELECT COALESCE(MAX(q.orderIndex), 0) + 1 FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    int findNextOrderIndex(@Param("quizId") Long quizId);
}
