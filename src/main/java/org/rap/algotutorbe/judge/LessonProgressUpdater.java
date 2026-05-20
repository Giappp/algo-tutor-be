package org.rap.algotutorbe.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for auto-updating lesson progress when a submission is ACCEPTED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonProgressUpdater {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Transactional
    public boolean markLessonCompleted(User user, CodingLesson codingLesson) {
        LessonProgress progress = lessonProgressRepository.findByUserAndLesson(user, codingLesson)
                .orElse(null);

        if (progress == null) {
            // User might not have a progress record — find enrollment and create one
            Topic topic = codingLesson.getTopic();
            if (topic == null) return false;
            LearningPath learningPath = topic.getLearningPath();
            if (learningPath == null) return false;

            Enrollment enrollment = enrollmentRepository
                    .findByUserAndLearningPathIdAndStatus(user, learningPath.getId(), EnrollmentStatus.IN_PROGRESS)
                    .orElse(null);
            if (enrollment == null) return false;

            progress = new LessonProgress();
            progress.setUser(user);
            progress.setLesson(codingLesson);
            progress.setEnrollment(enrollment);
        }

        if (Boolean.TRUE.equals(progress.getIsCompleted())) {
            return false; // Already completed before
        }

        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setIsCompleted(true);
        progress.setCompletedAt(Instant.now());
        lessonProgressRepository.save(progress);

        log.info("Auto-marked lesson [{}] as COMPLETED for user [{}]",
                codingLesson.getSlug(), user.getId());
        return true;
    }
}
