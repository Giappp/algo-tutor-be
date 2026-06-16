package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.dto.CurrentLessonResponse;
import org.rap.algotutorbe.iam.dto.EnrollmentProgressResponse;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.EnrollmentLastActivityProjection;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserEnrollmentService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Transactional(readOnly = true)
    public List<EnrollmentProgressResponse> getEnrollmentsSorted(UUID userId) {
        User user = getUserOrThrow(userId);

        List<Enrollment> enrollments = enrollmentRepository.findUserLearningEnrollments(user);

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<UUID> enrollmentIds = enrollments.stream()
                .map(Enrollment::getId)
                .toList();

        Map<UUID, Instant> lastActivityMap = lessonProgressRepository
                .findLastActivityByEnrollmentIds(enrollmentIds)
                .stream()
                .collect(Collectors.toMap(
                        EnrollmentLastActivityProjection::getEnrollmentId,
                        EnrollmentLastActivityProjection::getLastActivityAt
                ));

        return enrollments.stream()
                .sorted((a, b) -> {
                    Instant activityA = resolveActivityAt(a, lastActivityMap);
                    Instant activityB = resolveActivityAt(b, lastActivityMap);

                    return activityB.compareTo(activityA);
                })
                .map(this::buildEnrollmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CurrentLessonResponse getCurrentLesson(UUID userId) {
        User user = getUserOrThrow(userId);

        return enrollmentRepository.findActiveUserLearningEnrollments(user)
                .stream()
                .map(this::buildCurrentLessonCandidate)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(
                        CurrentLessonCandidate::lastActivityAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ))
                .map(CurrentLessonCandidate::response)
                .orElse(null);
    }

    private EnrollmentProgressResponse buildEnrollmentResponse(Enrollment enrollment) {
        LearningPath learningPath = enrollment.getLearningPath();

        Lesson nextLesson = findCurrentLesson(enrollment);

        return new EnrollmentProgressResponse(
                enrollment.getId(),
                learningPath.getId(),
                learningPath.getName(),
                learningPath.getSlug(),
                learningPath.getThumbnailUrl(),
                learningPath.getLevel() != null
                        ? learningPath.getLevel().name()
                        : null,
                enrollment.getStatus() != null
                        ? enrollment.getStatus().name()
                        : null,
                enrollment.getProgressPercentage() != null
                        ? enrollment.getProgressPercentage().intValue()
                        : 0.0,
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt(),
                nextLesson != null ? nextLesson.getSlug() : null,
                nextLesson != null ? nextLesson.getTitle() : null
        );
    }

    private CurrentLessonCandidate buildCurrentLessonCandidate(Enrollment enrollment) {
        LearningPath learningPath = enrollment.getLearningPath();

        if (learningPath == null || learningPath.getTopics() == null) {
            return null;
        }

        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollment(enrollment);
        Map<Long, LessonProgress> progressMap = progresses.stream()
                .filter(progress -> progress.getLesson() != null && progress.getLesson().getId() != null)
                .collect(Collectors.toMap(
                        progress -> progress.getLesson().getId(),
                        progress -> progress,
                        this::chooseNewestProgress
                ));

        List<Lesson> unlockedLessons = findUnlockedPublishedLessons(learningPath, progressMap);
        if (unlockedLessons.isEmpty()) {
            return null;
        }

        Lesson lesson = findInProgressLesson(unlockedLessons, progressMap)
                .orElseGet(() -> findFirstUncompletedLesson(unlockedLessons, progressMap));

        if (lesson == null) {
            return null;
        }

        CurrentLessonResponse response = new CurrentLessonResponse(
                learningPath.getSlug(),
                lesson.getSlug(),
                lesson.getTitle(),
                learningPath.getName(),
                calculateCompletionPercentage(learningPath, progressMap)
        );

        return new CurrentLessonCandidate(response, resolveActivityAt(enrollment, progresses));
    }

    private Instant resolveActivityAt(
            Enrollment enrollment,
            Map<UUID, Instant> lastActivityMap
    ) {
        Instant lastProgressAt = lastActivityMap.get(enrollment.getId());

        if (lastProgressAt != null) {
            return lastProgressAt;
        }

        if (enrollment.getUpdatedAt() != null) {
            return enrollment.getUpdatedAt();
        }

        return enrollment.getEnrolledAt();
    }

    private Instant resolveActivityAt(
            Enrollment enrollment,
            List<LessonProgress> progresses
    ) {
        return progresses.stream()
                .map(LessonProgress::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseGet(() -> {
                    if (enrollment.getUpdatedAt() != null) {
                        return enrollment.getUpdatedAt();
                    }
                    if (enrollment.getEnrolledAt() != null) {
                        return enrollment.getEnrolledAt();
                    }
                    return enrollment.getCreatedAt();
                });
    }

    private Lesson findCurrentLesson(Enrollment enrollment) {
        LearningPath learningPath = enrollment.getLearningPath();

        if (learningPath == null || learningPath.getTopics() == null) {
            return null;
        }

        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollment(enrollment);
        Map<Long, LessonProgress> progressMap = progresses.stream()
                .filter(progress -> progress.getLesson() != null && progress.getLesson().getId() != null)
                .collect(Collectors.toMap(
                        progress -> progress.getLesson().getId(),
                        progress -> progress,
                        this::chooseNewestProgress
                ));

        List<Lesson> unlockedLessons = findUnlockedPublishedLessons(learningPath, progressMap);

        return findInProgressLesson(unlockedLessons, progressMap)
                .orElseGet(() -> findFirstUncompletedLesson(unlockedLessons, progressMap));
    }

    private List<Lesson> findUnlockedPublishedLessons(
            LearningPath learningPath,
            Map<Long, LessonProgress> progressMap
    ) {
        List<Topic> sortedTopics = learningPath.getTopics().stream()
                .sorted(Comparator.comparing(Topic::getDisplayOrder))
                .toList();

        List<Lesson> unlockedLessons = new ArrayList<>();
        boolean previousTopicCompleted = true;

        for (Topic topic : sortedTopics) {
            List<Lesson> sortedLessons = getPublishedLessons(topic);
            boolean topicUnlocked = previousTopicCompleted;

            if (topicUnlocked) {
                unlockedLessons.addAll(sortedLessons);
            }

            previousTopicCompleted = isTopicCompleted(sortedLessons, progressMap);
        }

        return unlockedLessons;
    }

    private List<Lesson> getPublishedLessons(Topic topic) {
        if (topic.getLessons() == null) {
            return List.of();
        }

        return topic.getLessons().stream()
                .filter(lesson -> Boolean.TRUE.equals(lesson.getIsPublished()))
                .sorted(Comparator.comparing(Lesson::getDisplayOrder))
                .toList();
    }

    private boolean isTopicCompleted(
            List<Lesson> lessons,
            Map<Long, LessonProgress> progressMap
    ) {
        if (lessons.isEmpty()) {
            return false;
        }

        return lessons.stream()
                .allMatch(lesson -> getProgressStatus(progressMap, lesson) == ProgressStatus.COMPLETED);
    }

    private Optional<Lesson> findInProgressLesson(
            List<Lesson> lessons,
            Map<Long, LessonProgress> progressMap
    ) {
        return lessons.stream()
                .filter(lesson -> getProgressStatus(progressMap, lesson) == ProgressStatus.IN_PROGRESS)
                .max(Comparator.comparing(
                        lesson -> resolveProgressUpdatedAt(progressMap.get(lesson.getId())),
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ));
    }

    private Lesson findFirstUncompletedLesson(
            List<Lesson> lessons,
            Map<Long, LessonProgress> progressMap
    ) {
        return lessons.stream()
                .filter(lesson -> getProgressStatus(progressMap, lesson) != ProgressStatus.COMPLETED)
                .findFirst()
                .orElse(null);
    }

    private ProgressStatus getProgressStatus(
            Map<Long, LessonProgress> progressMap,
            Lesson lesson
    ) {
        LessonProgress progress = progressMap.get(lesson.getId());
        return progress != null ? progress.getEffectiveStatus() : ProgressStatus.NOT_STARTED;
    }

    private Instant resolveProgressUpdatedAt(LessonProgress progress) {
        if (progress == null) {
            return null;
        }
        if (progress.getUpdatedAt() != null) {
            return progress.getUpdatedAt();
        }
        return progress.getCreatedAt();
    }

    private LessonProgress chooseNewestProgress(
            LessonProgress first,
            LessonProgress second
    ) {
        Instant firstUpdatedAt = resolveProgressUpdatedAt(first);
        Instant secondUpdatedAt = resolveProgressUpdatedAt(second);

        if (firstUpdatedAt == null) {
            return second;
        }
        if (secondUpdatedAt == null) {
            return first;
        }

        return secondUpdatedAt.isAfter(firstUpdatedAt) ? second : first;
    }

    private int calculateCompletionPercentage(
            LearningPath learningPath,
            Map<Long, LessonProgress> progressMap
    ) {
        List<Lesson> lessons = learningPath.getTopics().stream()
                .flatMap(topic -> getPublishedLessons(topic).stream())
                .toList();

        if (lessons.isEmpty()) {
            return 0;
        }

        long completedLessons = lessons.stream()
                .filter(lesson -> getProgressStatus(progressMap, lesson) == ProgressStatus.COMPLETED)
                .count();

        long rounded = Math.round((completedLessons * 100.0) / lessons.size());
        return (int) Math.max(0, Math.min(100, rounded));
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private record CurrentLessonCandidate(
            CurrentLessonResponse response,
            Instant lastActivityAt
    ) {
    }
}
