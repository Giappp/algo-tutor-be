package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.ProgressStatus;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    @PreUpdate
    protected void syncFields() {
        // Keep isCompleted in sync with status for backward compatibility
        if (status == ProgressStatus.COMPLETED) {
            isCompleted = true;
            if (completedAt == null) {
                completedAt = Instant.now();
            }
        } else {
            isCompleted = false;
            completedAt = null;
        }
    }

    /**
     * Convenience method to get the effective progress status.
     * Derives from the status field, falling back to isCompleted for legacy data.
     */
    public ProgressStatus getEffectiveStatus() {
        if (status != null) return status;
        return Boolean.TRUE.equals(isCompleted) ? ProgressStatus.COMPLETED : ProgressStatus.NOT_STARTED;
    }
}
