package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT l FROM Lesson l WHERE l.slug = :slug")
    Optional<Lesson> findBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    List<Lesson> findByTopicIdOrderByOrderIndex(Long topicId);

    @Query("SELECT l FROM Lesson l WHERE l.topic.id = :topicId AND l.isPublished = true ORDER BY l.orderIndex")
    List<Lesson> findByTopicIdAndPublishedTrueOrderByOrderIndex(@Param("topicId") Long topicId);

    Optional<Lesson> findByIdAndTopicId(Long id, Long topicId);
}
