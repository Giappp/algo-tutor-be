package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonProgressUpdater {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final LearningAccessService learningAccessService;

    @Transactional
    public boolean markLessonCompleted(User user, Lesson lesson) {
        Topic topic = lesson.getTopic();

        if (topic == null || topic.getLearningPath() == null) {
            log.warn("Cannot complete lesson [{}] because topic or learning path is missing", lesson.getId());
            return false;
        }

        LearningPath learningPath = topic.getLearningPath();

        Enrollment enrollment = enrollmentRepository
                .findByUserAndLearningPathIdAndStatus(
                        user,
                        learningPath.getId(),
                        EnrollmentStatus.IN_PROGRESS
                )
                .orElse(null);

        if (enrollment == null) {
            log.warn(
                    "Cannot complete lesson [{}] for user [{}] because enrollment is missing",
                    lesson.getId(),
                    user.getId()
            );
            return false;
        }

        if (!learningAccessService.canAccessLesson(user.getId(), lesson.getId())) {
            log.warn(
                    "User [{}] tried to complete locked lesson [{}]",
                    user.getId(),
                    lesson.getId()
            );
            return false;
        }

        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentAndLesson(enrollment, lesson)
                .orElseGet(() -> createProgress(user, enrollment, lesson));

        if (progress.getStatus() == ProgressStatus.COMPLETED) {
            return false;
        }

        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setIsCompleted(true);
        progress.setCompletedAt(Instant.now());

        lessonProgressRepository.save(progress);

        log.info(
                "Marked lesson [{}] as COMPLETED for user [{}]",
                lesson.getSlug(),
                user.getId()
        );

        updateEnrollmentProgress(enrollment);
        return true;
    }

    private LessonProgress createProgress(User user, Enrollment enrollment, Lesson lesson) {
        LessonProgress progress = new LessonProgress();
        progress.setUser(user);
        progress.setEnrollment(enrollment);
        progress.setLesson(lesson);
        return progress;
    }

    @Transactional
    public void updateEnrollmentProgress(Enrollment enrollment) {
        LearningPath learningPath = enrollment.getLearningPath();

        if (learningPath == null) {
            return;
        }

        long totalLessons = lessonRepository
                .countPublishedLessonsByLearningPathId(learningPath.getId());

        if (totalLessons == 0) {
            enrollment.setProgressPercentage(0.0);
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
            enrollment.setCompletedAt(null);
            enrollmentRepository.save(enrollment);
            return;
        }

        long completedLessons = lessonProgressRepository
                .countCompletedByEnrollmentId(enrollment.getId());

        double percentage = calculatePercentage(completedLessons, totalLessons);

        enrollment.setProgressPercentage(percentage);

        if (completedLessons >= totalLessons) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);

            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(Instant.now());
            }

            log.info(
                    "User [{}] completed LearningPath [{}]",
                    enrollment.getUser().getId(),
                    learningPath.getName()
            );
        } else {
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
            enrollment.setCompletedAt(null);
        }

        enrollmentRepository.save(enrollment);
    }

    private double calculatePercentage(long completedLessons, long totalLessons) {
        return ((double) completedLessons / totalLessons) * 100.0;
    }
}
