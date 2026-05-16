package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;
import org.rap.algotutorbe.iam.domain.model.User;

import java.time.Instant;

@Entity
@Table(name = "lesson_progresses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class LessonProgress extends BaseUuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    @PreUpdate
    protected void syncCompletedAt() {
        if (Boolean.TRUE.equals(isCompleted) && completedAt == null) {
            completedAt = Instant.now();
        } else if (Boolean.FALSE.equals(isCompleted)) {
            completedAt = null;
        }
    }
}
