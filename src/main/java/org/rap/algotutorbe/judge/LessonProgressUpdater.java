package org.rap.algotutorbe.judge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.submission.entities.Submission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Shared service for auto-updating lesson progress when a submission is ACCEPTED.
 * Used by both the synchronous JudgeRunService and the async SubmissionListener.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonProgressUpdater {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    /**
     * Marks the lesson as completed for the submission's user.
     * Returns true if this was the first time marking it completed.
     */
    @Transactional
    public boolean markLessonCompletedIfNeeded(Submission submission) {
        User user = submission.getUser();
        CodingLesson codingLesson = submission.getCodingLesson();

        if (user == null || codingLesson == null) {
            log.warn("Cannot update progress: user or codingLesson is null for submission [{}]",
                    submission.getId());
            return false;
        }

        return markLessonCompleted(user, codingLesson);
    }

    /**
     * Marks the lesson as completed for the given user.
     * Returns true if this was the first time marking it completed.
     */
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
