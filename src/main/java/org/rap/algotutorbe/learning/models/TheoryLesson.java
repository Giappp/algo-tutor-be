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

    /**
     * Optional: manually set estimated reading time in minutes.
     * If null, the service will calculate it dynamically from content word count.
     */
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;
}
