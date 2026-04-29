package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.enums.LessonType;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonType type;

    private String content;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Convert(converter = DifficultyConverter.class)
    @Column(columnDefinition = "varchar(20)")
    private Difficulty difficulty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(unique = true)
    private String slug;
}
