package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.learning.mapper.QuestionType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
public class QuizQuestion extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private QuizLesson quiz;

    @Column(nullable = false)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type = QuestionType.SINGLE_CHOICE;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "points")
    private Integer points = 1;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "choice_order")
    private List<QuizChoice> choices = new ArrayList<>();

}
