package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;
import org.rap.algotutorbe.learning.models.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    boolean existsByUser(User user);

    boolean existsEnrollmentByUser(User user);

    boolean existsByUserAndLearningPathId(User user, Long learningPathId);

    Optional<Enrollment> findByUserAndLearningPathId(User user, Long learningPathId);

    Optional<Enrollment> findByUserAndLearningPathIdAndStatus(User user, Long learningPathId, EnrollmentStatus status);
}
