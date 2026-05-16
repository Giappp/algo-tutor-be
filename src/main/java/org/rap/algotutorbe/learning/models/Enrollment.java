package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "learning_path_id"})
})
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

    @Column(name = "progress_percentage")
    private Double progressPercentage = 0.0;

    @Column(name = "enrolled_at")
    private Instant enrolledAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.IN_PROGRESS;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LessonProgress> lessonProgressions = new LinkedHashSet<>();
}
