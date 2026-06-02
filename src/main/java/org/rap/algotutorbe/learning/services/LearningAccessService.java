package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningAccessService {
    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public boolean canAccessLesson(UUID userId, Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Topic topic = lesson.getTopic();
        LearningPath roadmap = topic.getLearningPath();

        List<Topic> topics = topicRepository.findByLearningPathIdOrderByDisplayOrder(roadmap.getId());

        Topic previousTopic = null;

        for (int i = 0; i < topics.size(); i++) {
            if (topics.get(i).getId().equals(topic.getId())) {
                if (i == 0) {
                    return true;
                }

                previousTopic = topics.get(i - 1);
                break;
            }
        }

        if (previousTopic == null) {
            return false;
        }

        return isTopicCompleted(userId, previousTopic.getId());
    }

    private boolean isTopicCompleted(UUID userId, Long topicId) {
        long totalLessons = lessonRepository.countByTopicId(topicId);

        if (totalLessons == 0) {
            return false;
        }

        long completedLessons = lessonProgressRepository
                .countCompletedLessonsByUserIdAndTopicId(userId, topicId);

        return totalLessons == completedLessons;
    }
}
