package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;
import org.rap.algotutorbe.learning.models.Enrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    boolean existsByUserIdAndLearningPathId(java.util.UUID userId, Long learningPathId);

    Optional<Enrollment> findByUserAndLearningPathId(User user, Long learningPathId);

    Optional<Enrollment> findByUserAndLearningPathIdAndStatus(User user, Long learningPathId, EnrollmentStatus status);

    @EntityGraph(attributePaths = {
            "learningPath",
            "learningPath.topics",
            "learningPath.topics.lessons"
    })
    @Query("""
                SELECT e
                FROM Enrollment e
                WHERE e.user = :user
                  AND e.status IN (
                      org.rap.algotutorbe.learning.enums.EnrollmentStatus.IN_PROGRESS,
                      org.rap.algotutorbe.learning.enums.EnrollmentStatus.COMPLETED
                  )
            """)
    List<Enrollment> findUserLearningEnrollments(@Param("user") User user);

    @EntityGraph(attributePaths = {
            "learningPath",
            "learningPath.topics",
            "learningPath.topics.lessons"
    })
    @Query("""
                SELECT e
                FROM Enrollment e
                WHERE e.user = :user
                  AND e.status = org.rap.algotutorbe.learning.enums.EnrollmentStatus.IN_PROGRESS
            """)
    List<Enrollment> findActiveUserLearningEnrollments(@Param("user") User user);
}
