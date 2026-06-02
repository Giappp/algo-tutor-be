package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.dto.EnrollmentProgressResponse;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.mapper.RoadmapMapper;
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
    private final RoadmapMapper roadmapMapper;

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

    private EnrollmentProgressResponse buildEnrollmentResponse(Enrollment enrollment) {
        LearningPath learningPath = enrollment.getLearningPath();

        Lesson nextLesson = findNextLesson(enrollment);

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
                        ? enrollment.getProgressPercentage()
                        : 0.0,
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt(),
                nextLesson != null ? nextLesson.getSlug() : null,
                nextLesson != null ? nextLesson.getTitle() : null
        );
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

    private Lesson findNextLesson(Enrollment enrollment) {
        LearningPath learningPath = enrollment.getLearningPath();

        if (learningPath == null || learningPath.getTopics() == null) {
            return null;
        }

        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollment(enrollment);

        Set<Long> completedLessonIds = progresses.stream()
                .filter(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .map(progress -> progress.getLesson().getId())
                .collect(Collectors.toSet());

        return findFirstUncompletedPublishedLesson(learningPath, completedLessonIds);
    }

    private Lesson findFirstUncompletedPublishedLesson(
            LearningPath learningPath,
            Set<Long> completedLessonIds
    ) {
        List<Topic> sortedTopics = learningPath.getTopics().stream()
                .sorted(Comparator.comparing(Topic::getDisplayOrder))
                .toList();

        for (Topic topic : sortedTopics) {
            if (topic.getLessons() == null) {
                continue;
            }

            List<Lesson> sortedLessons = topic.getLessons().stream()
                    .filter(lesson -> Boolean.TRUE.equals(lesson.getIsPublished()))
                    .sorted(Comparator.comparing(Lesson::getDisplayOrder))
                    .toList();

            for (Lesson lesson : sortedLessons) {
                if (!completedLessonIds.contains(lesson.getId())) {
                    return lesson;
                }
            }
        }

        return null;
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
