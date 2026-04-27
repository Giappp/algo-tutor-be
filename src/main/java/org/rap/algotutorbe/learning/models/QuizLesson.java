package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class QuizLesson extends Lesson {
    @Column(name = "passing_score")
    private Integer passingScore = 70;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttempt> attempts = new ArrayList<>();
}