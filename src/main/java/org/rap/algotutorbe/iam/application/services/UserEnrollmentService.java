package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.dto.CurrentLessonResponse;
import org.rap.algotutorbe.iam.dto.EnrollmentProgressResponse;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.mapper.RoadmapMapper;
import org.rap.algotutorbe.learning.models.*;
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

    public Optional<CurrentLessonResponse> getCurrentLesson(UUID userId) {
        User user = getUserOrThrow(userId);
        List<Enrollment> enrollments = enrollmentRepository.findActiveEnrollmentsWithLessons(user);

        if (enrollments.isEmpty()) {
            return Optional.empty();
        }

        return findInProgressLesson(user, enrollments)
                .or(() -> findFirstNotStartedLesson(enrollments));
    }

    public List<EnrollmentProgressResponse> getEnrollments(UUID userId) {
        User user = getUserOrThrow(userId);
        List<Enrollment> enrollments = enrollmentRepository.findActiveEnrollmentsWithLessons(user);

        if (enrollments.isEmpty()) {
            return List.of();
        }

        return enrollments.stream()
                .map(this::buildEnrollmentResponse)
                .sorted(Comparator.comparing(EnrollmentProgressResponse::completionPercentage).reversed())
                .toList();
    }

    public List<EnrollmentProgressResponse> getEnrollmentsSorted(UUID userId) {
        User user = getUserOrThrow(userId);
        List<Enrollment> enrollments = enrollmentRepository.findActiveEnrollmentsWithLessons(user);

        if (enrollments.isEmpty()) {
            return List.of();
        }

        return sortEnrollmentsByActivity(enrollments).stream()
                .map(this::buildEnrollmentResponse)
                .toList();
    }

    public List<RoadmapResponseDTO> getUserRoadmaps(UUID userId) {
        User user = getUserOrThrow(userId);
        if (user.getEnrollments() == null || user.getEnrollments().isEmpty()) {
            return List.of();
        }
        return user.getEnrollments().stream()
                .map(Enrollment::getLearningPath)
                .map(roadmapMapper::toResponse)
                .toList();
    }

    private Optional<CurrentLessonResponse> findInProgressLesson(User user, List<Enrollment> enrollments) {
        List<LessonProgress> inProgressLessons = lessonProgressRepository
                .findByUserAndStatusOrderByUpdatedAtDesc(user, ProgressStatus.IN_PROGRESS);

        if (inProgressLessons.isEmpty()) {
            return Optional.empty();
        }

        LessonProgress latest = inProgressLessons.getFirst();
        Lesson lesson = latest.getLesson();
        LearningPath roadmap = lesson.getTopic().getLearningPath();

        Enrollment enrollment = findEnrollmentForRoadmap(enrollments, roadmap.getId());
        int percentage = calculateCompletionPercentage(enrollment);

        return Optional.of(new CurrentLessonResponse(
                roadmap.getSlug(), lesson.getSlug(), lesson.getTitle(),
                roadmap.getName(), percentage
        ));
    }

    private Optional<CurrentLessonResponse> findFirstNotStartedLesson(List<Enrollment> enrollments) {
        Enrollment latestEnrollment = enrollments.stream()
                .max(Comparator.comparing(Enrollment::getCreatedAt))
                .orElse(null);

        if (latestEnrollment == null) {
            return Optional.empty();
        }

        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollment(latestEnrollment);
        Set<Long> progressedLessonIds = progresses.stream()
                .filter(lp -> lp.getStatus() == ProgressStatus.COMPLETED)
                .map(lp -> lp.getLesson().getId())
                .collect(Collectors.toSet());

        Lesson firstNotStarted = findFirstUncompletedLesson(latestEnrollment, progressedLessonIds);
        if (firstNotStarted == null) {
            return Optional.empty();
        }

        LearningPath roadmap = latestEnrollment.getLearningPath();
        int percentage = calculateCompletionPercentage(latestEnrollment);

        return Optional.of(new CurrentLessonResponse(
                roadmap.getSlug(), firstNotStarted.getSlug(), firstNotStarted.getTitle(),
                roadmap.getName(), percentage
        ));
    }

    private Lesson findFirstUncompletedLesson(Enrollment enrollment, Set<Long> completedLessonIds) {
        LearningPath roadmap = enrollment.getLearningPath();
        if (roadmap == null || roadmap.getTopics() == null) {
            return null;
        }
        List<Topic> sortedTopics = roadmap.getTopics().stream()
                .sorted(Comparator.comparing(Topic::getDisplayOrder))
                .toList();

        for (Topic topic : sortedTopics) {
            if (topic.getLessons() == null) {
                continue;
            }
            List<Lesson> sortedLessons = topic.getLessons().stream()
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

    private EnrollmentProgressResponse buildEnrollmentResponse(Enrollment enrollment) {
        LearningPath roadmap = enrollment.getLearningPath();
        int percentage = calculateCompletionPercentage(enrollment);

        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollment(enrollment);
        Set<Long> completedLessonIds = progresses.stream()
                .filter(lp -> lp.getStatus() == ProgressStatus.COMPLETED)
                .map(lp -> lp.getLesson().getId())
                .collect(Collectors.toSet());

        Lesson nextLesson = findNextLesson(enrollment, progresses, completedLessonIds);

        return new EnrollmentProgressResponse(
                roadmap.getName(), roadmap.getSlug(), percentage,
                nextLesson != null ? nextLesson.getSlug() : null,
                nextLesson != null ? nextLesson.getTitle() : null,
                roadmap.getThumbnailUrl()
        );
    }

    private Lesson findNextLesson(Enrollment enrollment, List<LessonProgress> progresses, Set<Long> completedIds) {
        Optional<LessonProgress> inProgress = progresses.stream()
                .filter(lp -> lp.getStatus() == ProgressStatus.IN_PROGRESS)
                .max(Comparator.comparing(LessonProgress::getUpdatedAt));

        if (inProgress.isPresent()) {
            return inProgress.get().getLesson();
        }

        return findFirstUncompletedLesson(enrollment, completedIds);
    }

    private List<Enrollment> sortEnrollmentsByActivity(List<Enrollment> enrollments) {
        Map<UUID, Instant> latestActivityMap = new HashMap<>();

        for (Enrollment enrollment : enrollments) {
            List<LessonProgress> progresses = lessonProgressRepository.findByEnrollment(enrollment);
            Instant latestUpdate = progresses.stream()
                    .map(LessonProgress::getUpdatedAt)
                    .filter(Objects::nonNull)
                    .max(Instant::compareTo)
                    .orElse(null);

            latestActivityMap.put(enrollment.getId(), latestUpdate != null ? latestUpdate : enrollment.getCreatedAt());
        }

        return enrollments.stream()
                .sorted(Comparator.comparing(
                        (Enrollment e) -> latestActivityMap.get(e.getId())
                ).reversed())
                .toList();
    }

    private int calculateCompletionPercentage(Enrollment enrollment) {
        if (enrollment.getLearningPath() == null) {
            return 0;
        }
        int totalLessons = countTotalLessons(enrollment.getLearningPath());
        if (totalLessons == 0) return 0;

        long completedLessons = lessonProgressRepository.findCompletedByEnrollment(enrollment).size();
        return (int) Math.floor((double) completedLessons / totalLessons * 100);
    }

    private int countTotalLessons(LearningPath roadmap) {
        if (roadmap == null || roadmap.getTopics() == null) {
            return 0;
        }
        return roadmap.getTopics().stream()
                .filter(topic -> topic.getLessons() != null)
                .mapToInt(topic -> topic.getLessons().size())
                .sum();
    }

    private Enrollment findEnrollmentForRoadmap(List<Enrollment> enrollments, Long roadmapId) {
        return enrollments.stream()
                .filter(e -> e.getLearningPath().getId().equals(roadmapId))
                .findFirst()
                .orElse(enrollments.getFirst());
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
