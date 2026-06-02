package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.models.Enrollment;
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

    @Query("SELECT lp FROM LessonProgress lp WHERE lp.user = :user AND lp.lesson IN :lessons")
    List<LessonProgress> findAllByUserAndLessons(@Param("user") User user, @Param("lessons") List<Lesson> lessons);

    @Query("""
                SELECT COUNT(lp)
                FROM LessonProgress lp
                JOIN lp.lesson l
                WHERE lp.user.id = :userId
                  AND l.topic.id = :topicId
                  AND lp.status = 'COMPLETED'
            """)
    long countCompletedLessonsByUserIdAndTopicId(
            @Param("userId") UUID userId,
            @Param("topicId") Long topicId
    );

    List<LessonProgress> findByEnrollment(Enrollment enrollment);

    Optional<LessonProgress> findByEnrollmentAndLesson(
            Enrollment enrollment,
            Lesson lesson
    );

    @Query("""
                SELECT COUNT(lp)
                FROM LessonProgress lp
                WHERE lp.enrollment.id = :enrollmentId
                  AND lp.status = org.rap.algotutorbe.learning.enums.ProgressStatus.COMPLETED
            """)
    long countCompletedByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

    @Query("""
                SELECT lp.enrollment.id AS enrollmentId,
                       MAX(lp.updatedAt) AS lastActivityAt
                FROM LessonProgress lp
                WHERE lp.enrollment.id IN :enrollmentIds
                GROUP BY lp.enrollment.id
            """)
    List<EnrollmentLastActivityProjection> findLastActivityByEnrollmentIds(
            @Param("enrollmentIds") List<UUID> enrollmentIds
    );
}
