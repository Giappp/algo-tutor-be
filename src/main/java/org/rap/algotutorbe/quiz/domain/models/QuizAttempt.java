package org.rap.algotutorbe.quiz.domain.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;
import org.rap.algotutorbe.iam.domain.model.User;

import java.time.Instant;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt extends BaseUuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "score")
    private Float score;

    @Column(name = "total_points")
    private Integer totalPoints;

    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * JSON snapshot of answers: [{"questionId": 1, "selectedChoiceIndex": 0, "pointsEarned": 1}, ...]
     */
    @Column(name = "answers_snapshot", columnDefinition = "TEXT")
    private String answersSnapshot;
}
