package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;

import java.time.Instant;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
public class Enrollment extends BaseUuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;


    @Column(name = "completed_at")
    private Instant completedAt;
}
