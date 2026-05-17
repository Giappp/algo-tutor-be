package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByLearningPathIdOrderByDisplayOrder(Long learningPathId);

    @Query("SELECT COALESCE(MAX(t.displayOrder), 0) + 1 FROM Topic t WHERE t.learningPath.id = :learningPathId")
    int getNextDisplayOrder(@Param("learningPathId") Long learningPathId);
}
