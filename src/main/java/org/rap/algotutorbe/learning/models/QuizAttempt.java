package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;
import org.rap.algotutorbe.iam.domain.model.User;

import java.time.Instant;
import java.util.List;

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
    private QuizLesson quiz;

    private String lessonSlug;

    private String roadmapSlug;

    @Column(name = "score")
    private Float score;

    @Column(name = "is_passed", nullable = false)
    private Boolean passed;

    private Integer totalQuestions;

    private Integer correctCount;
    /**
     * JSON snapshot of answers: [{"questionId": 1, "selectedChoiceIndex": 0, "pointsEarned": 1}, ...]
     */
    @Column(name = "answers_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<QuestionAnswer> answersSnapshot;
    @Column(name = "question_results", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<QuestionResult> questionResults;

    private Integer attemptNumber;

    @Column(name = "total_points")
    private Integer totalPoints;


    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "completed_at")
    private Instant completedAt;

}
