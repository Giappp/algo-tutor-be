package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;
import org.rap.algotutorbe.learning.enums.Level;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "learning_paths")
@Entity
public class LearningPath extends BaseEntity {
    private String name;

    private String slug;
    @Enumerated(EnumType.STRING)
    private Level level;

    private String description;

    private String goal;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    private Boolean isPremium = false;

    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private Set<Topic> topics = new LinkedHashSet<>();

    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Enrollment> enrollments = new LinkedHashSet<>();
}
