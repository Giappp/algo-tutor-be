package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByLearningPathIdOrderByOrderIndex(Long learningPathId);

    List<Topic> findByLearningPathIdOrderByOrderIndexAsc(Long learningPathId);

    Optional<Topic> findByIdAndLearningPathId(Long id, Long learningPathId);
}
