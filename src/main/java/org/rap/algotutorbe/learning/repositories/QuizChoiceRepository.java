package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizChoiceRepository extends JpaRepository<QuizChoice, Long> {

    List<QuizChoice> findByQuestionIdOrderById(Long questionId);
}
