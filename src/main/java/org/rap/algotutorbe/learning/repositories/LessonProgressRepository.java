package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByUserAndLesson(User user, Lesson lesson);

    List<LessonProgress> findByUserAndLessonIn(User user, List<Lesson> lessons);

    boolean existsByUserAndLesson(User user, Lesson lesson);

    @Query("SELECT lp FROM LessonProgress lp WHERE lp.user = :user AND lp.lesson IN :lessons")
    List<LessonProgress> findAllByUserAndLessons(@Param("user") User user, @Param("lessons") List<Lesson> lessons);
}
