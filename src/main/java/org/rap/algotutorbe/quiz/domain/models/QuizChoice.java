package org.rap.algotutorbe.quiz.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Table(name = "quiz_answers")
@Getter
@Setter
@NoArgsConstructor
public class QuizChoice {

    @Column(name = "choice_text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    /**
     * Explanation shown to user after submitting — only for the correct answer.
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
}

