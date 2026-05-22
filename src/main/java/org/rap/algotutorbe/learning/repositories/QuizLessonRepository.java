package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.QuizLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizLessonRepository extends JpaRepository<QuizLesson, Long> {
    Optional<QuizLesson> findBySlug(String slug);
}
