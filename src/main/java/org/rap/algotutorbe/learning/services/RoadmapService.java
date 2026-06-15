package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.*;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;
import org.rap.algotutorbe.learning.enums.Level;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.mapper.RoadmapMapper;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoadmapService extends BaseService {
    private final LearningPathRepository learningPathRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;
    private final RoadmapMapper roadmapMapper;
    private final LessonProgressUpdater lessonProgressUpdater;

    @Transactional(readOnly = true)
    public PageResponse<RoadmapResponseDTO> getPublishedRoadmaps(Pageable pageable, String level) {
        Level levelEnum = null;
        if (level != null && !level.isBlank()) {
            try {
                levelEnum = Level.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_PROGRESS_STATUS);
            }
        }
        Page<LearningPath> page = learningPathRepository.findPublishedByLevel(levelEnum, pageable);
        return PageResponse.of(page.map(roadmapMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ApiResponse<RoadmapDetailResponseDTO> getRoadmapBySlug(String slug) {
        LearningPath learningPath = getLearningPath(slug);

        boolean enrolled = false;
        User user = null;
        Optional<SecurityUser> securityUserOpt = getCurrentUser();
        if (securityUserOpt.isPresent()) {
            user = securityUserOpt.get().getUser();
            enrolled = enrollmentRepository.existsByUserIdAndLearningPathId(user.getId(), learningPath.getId());
        }

        RoadmapDetailResponseDTO dto = buildDetailDto(learningPath, enrolled, user);

        return ApiResponse.buildSuccess(dto);
    }

    @Transactional
    public ApiResponse<EnrollmentResponseDTO> enroll(String slug) {
        LearningPath learningPath = getLearningPath(slug);

        if (!Boolean.TRUE.equals(learningPath.getIsPublished())) {
            throw new AppException(ErrorCode.LEARNING_PATH_NOT_PUBLISHED);
        }

        UUID userId = getCurrentUserIdOrThrow();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Optional<Enrollment> existing = enrollmentRepository
                .findByUserAndLearningPathId(user, learningPath.getId());

        if (existing.isPresent()) {
            return ApiResponse.buildSuccess(toEnrollmentResponse(existing.get()));
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setLearningPath(learningPath);
        enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
        enrollment.setProgressPercentage(0.0);
        enrollment.setCompletedAt(null);
        enrollment.setEnrolledAt(Instant.now());

        Enrollment saved = enrollmentRepository.save(enrollment);

        return ApiResponse.buildSuccess(toEnrollmentResponse(saved));
    }

    @Transactional
    public ApiResponse<LessonProgressUpdateResponse> updateLessonProgress(
            String slug, String lessonSlug, ProgressStatus status) {
        LearningPath learningPath = getLearningPath(slug);

        var securityUser = getCurrentUser().orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        Enrollment enrollment = enrollmentRepository
                .findByUserAndLearningPathId(securityUser.getUser(), learningPath.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ENROLLED));

        Lesson lesson = findLessonInLearningPath(learningPath, lessonSlug);
        if (lesson instanceof VideoLesson) {
            throw new AppException(ErrorCode.VIDEO_PROGRESS_MANAGED_AUTOMATICALLY);
        }

        if (!canAccessLesson(learningPath, lesson, securityUser.getUser())) {
            throw new AppException(ErrorCode.LESSON_LOCKED);
        }

        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentAndLesson(enrollment, lesson)
                .orElseGet(() -> {
                    LessonProgress lp = new LessonProgress();
                    lp.setUser(securityUser.getUser());
                    lp.setEnrollment(enrollment);
                    lp.setLesson(lesson);
                    return lp;
                });

        progress.setStatus(status);

        boolean completed = status == ProgressStatus.COMPLETED;
        progress.setIsCompleted(completed);
        progress.setCompletedAt(completed ? Instant.now() : null);

        lessonProgressRepository.save(progress);

        lessonProgressUpdater.updateEnrollmentProgress(enrollment);

        return ApiResponse.buildSuccess(new LessonProgressUpdateResponse(
                lesson.getId(),
                learningPath.getId(),
                status,
                Instant.now()
        ));
    }

    private boolean canAccessLesson(LearningPath learningPath, Lesson lesson, User user) {
        List<Topic> orderedTopics = learningPath.getTopics() != null
                ? learningPath.getTopics().stream()
                .sorted(Comparator.comparing(Topic::getDisplayOrder))
                .toList()
                : List.of();

        List<Lesson> allLessons = orderedTopics.stream()
                .flatMap(topic -> topic.getLessons() != null
                        ? topic.getLessons().stream()
                        : java.util.stream.Stream.empty())
                .toList();

        List<LessonProgress> progresses = lessonProgressRepository.findAllByUserAndLessons(user, allLessons);

        Map<Long, ProgressStatus> progressMap = new HashMap<>();
        for (LessonProgress progress : progresses) {
            progressMap.put(progress.getLesson().getId(), progress.getEffectiveStatus());
        }

        boolean previousTopicCompleted = true;

        for (Topic topic : orderedTopics) {
            boolean topicUnlocked = previousTopicCompleted;

            if (topic.getId().equals(lesson.getTopic().getId())) {
                return topicUnlocked;
            }

            TopicProgressState state = calculateTopicProgressState(topic, progressMap);
            previousTopicCompleted = state.completed();
        }

        return false;
    }

    @Transactional(readOnly = true)
    public ApiResponse<EnrollmentDetailResponseDTO> getEnrollment(String slug) {
        LearningPath learningPath = getLearningPath(slug);

        UUID userId = getCurrentUserIdOrThrow();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Enrollment enrollment = enrollmentRepository
                .findByUserAndLearningPathId(user, learningPath.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ENROLLED));

        List<LessonProgressionDTO> progressions = enrollment.getLessonProgressions().stream()
                .map(lp -> {
                    ProgressStatus status = lp.getEffectiveStatus();
                    return new LessonProgressionDTO(
                            lp.getLesson().getId(),
                            status,
                            lp.getUpdatedAt());
                })
                .toList();

        EnrollmentDetailResponseDTO dto = new EnrollmentDetailResponseDTO(
                enrollment.getId(),
                user.getId(),
                learningPath.getId(),
                learningPath.getName(),
                enrollment.getStatus(),
                enrollment.getCompletedAt(),
                enrollment.getEnrolledAt(),
                progressions);

        return ApiResponse.buildSuccess(dto);
    }

    private RoadmapDetailResponseDTO buildDetailDto(LearningPath learningPath, boolean enrolled, User user) {
        List<Topic> orderedTopics = learningPath.getTopics() != null
                ? learningPath.getTopics().stream()
                .sorted(Comparator.comparing(Topic::getDisplayOrder))
                .toList()
                : List.of();

        Map<Long, ProgressStatus> progressMap = new HashMap<>();

        if (enrolled && user != null) {
            List<Lesson> allLessons = orderedTopics.stream()
                    .flatMap(topic -> topic.getLessons() != null
                            ? topic.getLessons().stream()
                            : java.util.stream.Stream.empty())
                    .toList();

            if (!allLessons.isEmpty()) {
                List<LessonProgress> progresses = lessonProgressRepository.findAllByUserAndLessons(user, allLessons);

                for (LessonProgress progress : progresses) {
                    progressMap.put(progress.getLesson().getId(), progress.getEffectiveStatus());
                }
            }
        }

        List<TopicWithLessonsDTO> topicDtos = buildTopicDtosWithUnlockState(
                orderedTopics,
                progressMap,
                enrolled
        );

        int lessonCount = orderedTopics.stream()
                .mapToInt(topic -> topic.getLessons() != null ? topic.getLessons().size() : 0)
                .sum();

        int enrollmentCount = learningPathRepository.countActiveEnrollments(learningPath.getId());

        return new RoadmapDetailResponseDTO(
                learningPath.getId(),
                learningPath.getName(),
                learningPath.getSlug(),
                learningPath.getLevel() != null ? learningPath.getLevel().name() : null,
                learningPath.getDescription(),
                learningPath.getGoal(),
                learningPath.getThumbnailUrl(),
                learningPath.getIsPublished(),
                learningPath.getIsPremium(),
                enrollmentCount,
                orderedTopics.size(),
                lessonCount,
                enrolled,
                learningPath.getCreatedAt(),
                learningPath.getUpdatedAt(),
                topicDtos
        );
    }

    private List<TopicWithLessonsDTO> buildTopicDtosWithUnlockState(
            List<Topic> orderedTopics,
            Map<Long, ProgressStatus> progressMap,
            boolean enrolled
    ) {
        List<TopicWithLessonsDTO> result = new ArrayList<>();

        boolean previousTopicCompleted = true;

        for (Topic topic : orderedTopics) {
            TopicProgressState state = calculateTopicProgressState(topic, progressMap);

            boolean topicUnlocked = enrolled && previousTopicCompleted;

            List<LessonWithProgressDTO> lessonDtos = buildLessonDtosWithUnlockState(
                    topic,
                    progressMap,
                    topicUnlocked
            );

            result.add(new TopicWithLessonsDTO(
                    topic.getId(),
                    topic.getName(),
                    topic.getDescription(),
                    topic.getDisplayOrder(),
                    state.totalLessons(),
                    topicUnlocked,
                    state.completed(),
                    state.completedLessons(),
                    state.totalLessons(),
                    topic.getCreatedAt(),
                    topic.getUpdatedAt(),
                    lessonDtos
            ));

            previousTopicCompleted = state.completed();
        }

        return result;
    }

    private List<LessonWithProgressDTO> buildLessonDtosWithUnlockState(
            Topic topic,
            Map<Long, ProgressStatus> progressMap,
            boolean topicUnlocked
    ) {
        List<Lesson> orderedLessons = topic.getLessons() != null
                ? topic.getLessons().stream()
                .filter(lesson -> Boolean.TRUE.equals(lesson.getIsPublished()))
                .sorted(Comparator.comparing(Lesson::getDisplayOrder))
                .toList()
                : List.of();

        return orderedLessons.stream()
                .map(lesson -> new LessonWithProgressDTO(
                        lesson.getId(),
                        lesson.getTitle(),
                        lesson.getSlug(),
                        lesson.getType(),
                        lesson.getDisplayOrder(),
                        lesson.getDifficulty(),
                        progressMap.getOrDefault(lesson.getId(), null),
                        topicUnlocked,
                        lesson.getCreatedAt(),
                        lesson.getUpdatedAt()
                ))
                .toList();
    }

    private Lesson findLessonInLearningPath(LearningPath learningPath, String lessonSlug) {
        if (learningPath.getTopics() == null) {
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }
        return learningPath.getTopics().stream()
                .filter(t -> t.getLessons() != null)
                .flatMap(t -> t.getLessons().stream())
                .filter(l -> l.getSlug().equals(lessonSlug))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }

    private TopicProgressState calculateTopicProgressState(
            Topic topic,
            Map<Long, ProgressStatus> progressMap
    ) {
        List<Lesson> publishedLessons = topic.getLessons() != null
                ? topic.getLessons().stream()
                .filter(lesson -> Boolean.TRUE.equals(lesson.getIsPublished()))
                .toList()
                : List.of();

        int totalLessons = publishedLessons.size();

        int completedLessons = (int) publishedLessons.stream()
                .filter(lesson -> progressMap.get(lesson.getId()) == ProgressStatus.COMPLETED)
                .count();

        boolean completed = totalLessons > 0 && completedLessons == totalLessons;

        return new TopicProgressState(
                totalLessons,
                completedLessons,
                completed
        );
    }

    private EnrollmentResponseDTO toEnrollmentResponse(Enrollment enrollment) {
        return new EnrollmentResponseDTO(
                enrollment.getId().toString(),
                enrollment.getUser().getId(),
                enrollment.getLearningPath().getId(),
                enrollment.getLearningPath().getName(),
                enrollment.getStatus(),
                enrollment.getCompletedAt(),
                enrollment.getEnrolledAt());
    }

    @Transactional
    public ApiResponse<LessonProgressUpdateResponse> startLesson(String slug, String lessonSlug) {
        LearningPath learningPath = getLearningPath(slug);

        UUID userId = getCurrentUserIdOrThrow();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Enrollment enrollment = enrollmentRepository
                .findByUserAndLearningPathId(user, learningPath.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ENROLLED));

        Lesson lesson = findLessonInLearningPath(learningPath, lessonSlug);

        if (!canAccessLesson(learningPath, lesson, user)) {
            throw new AppException(ErrorCode.LESSON_LOCKED);
        }

        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentAndLesson(enrollment, lesson)
                .orElseGet(() -> {
                    LessonProgress lp = new LessonProgress();
                    lp.setUser(user);
                    lp.setEnrollment(enrollment);
                    lp.setLesson(lesson);
                    lp.setIsCompleted(false);
                    lp.setCompletedAt(null);
                    return lp;
                });

        if (progress.getStatus() != ProgressStatus.COMPLETED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
            progress.setIsCompleted(false);
            progress.setCompletedAt(null);
        }

        lessonProgressRepository.save(progress);

        return ApiResponse.buildSuccess(new LessonProgressUpdateResponse(
                lesson.getId(),
                learningPath.getId(),
                progress.getStatus(),
                Instant.now()
        ));
    }

    private LearningPath getLearningPath(String slug) {
        return learningPathRepository.findBySlugWithTopicsAndLessons(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
    }

    private record TopicProgressState(
            int totalLessons,
            int completedLessons,
            boolean completed
    ) {
    }
}
