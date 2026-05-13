package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT l FROM Lesson l WHERE l.slug = :slug")
    Optional<Lesson> findBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT COALESCE(MAX(l.displayOrder), 0) + 1 FROM Lesson l WHERE l.topic.id = :topicId")
    int getNextDisplayOrder(@Param("topicId") Long topicId);

    @Query("SELECT l FROM Lesson l WHERE l.topic.id = :topicId ORDER BY l.displayOrder")
    Page<Lesson> findByTopicIdOrderByOrderIndex(@Param("topicId") Long topicId, Pageable pageable);
}
