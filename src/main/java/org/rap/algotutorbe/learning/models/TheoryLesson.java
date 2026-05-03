package org.rap.algotutorbe.learning.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TheoryLesson extends Lesson {
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
}
