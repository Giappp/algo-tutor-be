package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "topics", indexes = {
    @Index(name = "idx_topics_learning_path_id", columnList = "learning_path_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Topic extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private Set<Lesson> lessons = new LinkedHashSet<>();
}
