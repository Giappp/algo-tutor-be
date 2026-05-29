package org.rap.algotutorbe.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service for auto-updating lesson progress and managing path completion/unlock triggers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonProgressUpdater {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final TopicRepository topicRepository;

    @Transactional
    public boolean markLessonCompleted(User user, Lesson lesson) {
        LessonProgress progress = lessonProgressRepository.findByUserAndLesson(user, lesson)
                .orElse(null);

        Topic topic = lesson.getTopic();
        if (topic == null) return false;
        LearningPath learningPath = topic.getLearningPath();
        if (learningPath == null) return false;

        Enrollment enrollment = null;

        if (progress == null) {
            // User might not have a progress record — find enrollment and create one
            enrollment = enrollmentRepository
                    .findByUserAndLearningPathIdAndStatus(user, learningPath.getId(), EnrollmentStatus.IN_PROGRESS)
                    .orElse(null);
            if (enrollment == null) return false;

            progress = new LessonProgress();
            progress.setUser(user);
            progress.setLesson(lesson);
            progress.setEnrollment(enrollment);
        } else {
            enrollment = progress.getEnrollment();
        }

        if (Boolean.TRUE.equals(progress.getIsCompleted())) {
            return false; // Already completed before
        }

        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setIsCompleted(true);
        progress.setCompletedAt(Instant.now());
        lessonProgressRepository.save(progress);

        log.info("Auto-marked lesson [{}] as COMPLETED for user [{}]",
                lesson.getSlug(), user.getId());

        // 1. Auto-unlock the next topic if applicable
        unlockNextTopicIfAllLessonsCompleted(learningPath, topic, user);

        // 2. Recalculate roadmap completion percentage & status
        if (enrollment != null) {
            updateEnrollmentProgress(enrollment);
        }

        return true;
    }

    private void unlockNextTopicIfAllLessonsCompleted(LearningPath learningPath, Topic completedTopic, User user) {
        List<Topic> orderedTopics = learningPath.getTopics().stream()
                .sorted(Comparator.comparing(Topic::getDisplayOrder))
                .toList();

        List<Lesson> completedTopicLessons = completedTopic.getLessons().stream()
                .sorted(Comparator.comparing(Lesson::getDisplayOrder))
                .toList();

        boolean allCompleted = completedTopicLessons.stream()
                .allMatch(lesson -> {
                    Optional<LessonProgress> progress = lessonProgressRepository.findByUserAndLesson(user, lesson);
                    return progress.isPresent() && Boolean.TRUE.equals(progress.get().getIsCompleted());
                });

        if (!allCompleted) {
            return;
        }

        int currentIndex = orderedTopics.indexOf(completedTopic);
        if (currentIndex < orderedTopics.size() - 1) {
            Topic nextTopic = orderedTopics.get(currentIndex + 1);
            if (Boolean.TRUE.equals(nextTopic.getIsLocked())) {
                nextTopic.setIsLocked(false);
                topicRepository.save(nextTopic);
                log.info("Auto-unlocked next topic: [{}] for user [{}]", nextTopic.getName(), user.getId());
            }
        }
    }

    public void updateEnrollmentProgress(Enrollment enrollment) {
        LearningPath learningPath = enrollment.getLearningPath();

        // Count total published lessons in this path
        long totalLessons = learningPath.getTopics().stream()
                .flatMap(t -> t.getLessons().stream())
                .filter(Lesson::getIsPublished)
                .count();

        if (totalLessons == 0) return;

        // Count completed lessons for this enrollment
        long completedCount = lessonProgressRepository.findCompletedByEnrollment(enrollment).size();

        double percentage = ((double) completedCount / totalLessons) * 100.0;
        enrollment.setProgressPercentage(percentage);

        if (percentage >= 100.0) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(Instant.now());
            log.info("User [{}] COMPLETED LearningPath [{}]!", enrollment.getUser().getId(), learningPath.getName());
        } else {
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
            enrollment.setCompletedAt(null);
        }

        enrollmentRepository.save(enrollment);
    }
}
