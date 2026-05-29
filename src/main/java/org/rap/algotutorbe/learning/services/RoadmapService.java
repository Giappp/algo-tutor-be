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
import org.rap.algotutorbe.judge.LessonProgressUpdater;
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
        Optional<LearningPath> published = learningPathRepository.findBySlugWithTopicsAndLessons(slug);
        if (published.isEmpty()) {
            Optional<LearningPath> exists = learningPathRepository.findBySlug(slug);
            if (exists.isPresent()) {
                throw new AppException(ErrorCode.LEARNING_PATH_NOT_PUBLISHED);
            }
            throw new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND);
        }
        LearningPath learningPath = published.get();

        UUID userId;
        User user = null;
        boolean enrolled = false;

        Optional<SecurityUser> currentUser = getCurrentUser();
        if (currentUser.isPresent()) {
            userId = currentUser.get().getId();
            user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                enrolled = enrollmentRepository
                        .findByUserAndLearningPathIdAndStatus(user, learningPath.getId(), EnrollmentStatus.IN_PROGRESS)
                        .isPresent();
            }
        }

        RoadmapDetailResponseDTO dto = buildDetailDto(learningPath, enrolled, user);

        return ApiResponse.buildSuccess(dto);
    }

    @Transactional
    public ApiResponse<EnrollmentResponseDTO> enroll(String slug) {
        LearningPath learningPath = learningPathRepository.findBySlugWithTopicsAndLessons(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));

        UUID userId = getCurrentUserIdOrThrow();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Optional<Enrollment> existing = enrollmentRepository.findByUserAndLearningPathId(user, learningPath.getId());

        if (existing.isPresent()) {
            return ApiResponse.buildSuccess(toEnrollmentResponse(existing.get()));
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setLearningPath(learningPath);
        enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
        enrollment.setEnrolledAt(Instant.now());
        Enrollment saved = enrollmentRepository.save(enrollment);

        initializeLessonProgressions(saved, learningPath, user);

        return ApiResponse.buildSuccess(toEnrollmentResponse(saved));
    }

    @Transactional
    public ApiResponse<LessonProgressUpdateResponse> updateLessonProgress(
            String slug, String lessonSlug, ProgressStatus status) {
        LearningPath learningPath = learningPathRepository.findBySlugWithTopicsAndLessons(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));

        UUID userId = getCurrentUserIdOrThrow();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Enrollment enrollment = enrollmentRepository
                .findByUserAndLearningPathIdAndStatus(user, learningPath.getId(), EnrollmentStatus.IN_PROGRESS)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ENROLLED));

        Lesson lesson = findLessonInLearningPath(learningPath, lessonSlug);

        Topic topic = lesson.getTopic();
        if (Boolean.TRUE.equals(topic.getIsLocked())) {
            throw new AppException(ErrorCode.TOPIC_LOCKED);
        }

        if (status == ProgressStatus.COMPLETED) {
            lessonProgressUpdater.markLessonCompleted(user, lesson);
        } else {
            LessonProgress progress = lessonProgressRepository.findByUserAndLesson(user, lesson)
                    .orElseGet(() -> {
                        LessonProgress lp = new LessonProgress();
                        lp.setUser(user);
                        lp.setLesson(lesson);
                        return lp;
                    });
            progress.setEnrollment(enrollment);
            progress.setStatus(status);
            progress.setIsCompleted(false);
            progress.setCompletedAt(null);
            lessonProgressRepository.save(progress);

            lessonProgressUpdater.updateEnrollmentProgress(enrollment);
        }

        return ApiResponse.buildSuccess(new LessonProgressUpdateResponse(
                lesson.getId(),
                status,
                Instant.now()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<EnrollmentDetailResponseDTO> getEnrollment(String slug) {
        LearningPath learningPath = learningPathRepository.findBySlugWithTopicsAndLessons(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));

        UUID userId = getCurrentUserIdOrThrow();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Enrollment enrollment = enrollmentRepository
                .findByUserAndLearningPathIdAndStatus(user, learningPath.getId(), EnrollmentStatus.IN_PROGRESS)
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
                    .flatMap(t -> t.getLessons() != null ? t.getLessons().stream() : java.util.stream.Stream.empty())
                    .toList();
            List<LessonProgress> progresses = lessonProgressRepository.findAllByUserAndLessons(user, allLessons);
            for (LessonProgress lp : progresses) {
                progressMap.put(lp.getLesson().getId(), toProgressStatus(lp));
            }
        }

        List<TopicWithLessonsDTO> topicDtos = orderedTopics.stream()
                .map(topic -> buildTopicDto(topic, progressMap))
                .toList();

        int lessonCount = orderedTopics.stream()
                .mapToInt(t -> t.getLessons() != null ? t.getLessons().size() : 0)
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
                topicDtos);
    }

    private TopicWithLessonsDTO buildTopicDto(Topic topic, Map<Long, ProgressStatus> progressMap) {
        List<Lesson> orderedLessons = topic.getLessons() != null
                ? topic.getLessons().stream()
                        .sorted(Comparator.comparing(Lesson::getDisplayOrder))
                        .toList()
                : List.of();

        List<LessonWithProgressDTO> lessonDtos = orderedLessons.stream()
                .map(lesson -> new LessonWithProgressDTO(
                        lesson.getId(),
                        lesson.getTitle(),
                        lesson.getSlug(),
                        lesson.getType(),
                        lesson.getDisplayOrder(),
                        lesson.getDifficulty(),
                        progressMap.getOrDefault(lesson.getId(), null),
                        lesson.getCreatedAt(),
                        lesson.getUpdatedAt()))
                .toList();

        return new TopicWithLessonsDTO(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getDisplayOrder(),
                topic.getIsLocked(),
                orderedLessons.size(),
                topic.getCreatedAt(),
                topic.getUpdatedAt(),
                lessonDtos);
    }

    private void initializeLessonProgressions(Enrollment enrollment, LearningPath learningPath, User user) {
        List<Topic> orderedTopics = learningPath.getTopics() != null
                ? learningPath.getTopics().stream()
                        .sorted(Comparator.comparing(Topic::getDisplayOrder))
                        .toList()
                : List.of();

        for (Topic topic : orderedTopics) {
            if (Boolean.FALSE.equals(topic.getIsLocked()) && topic.getLessons() != null) {
                List<Lesson> lessons = topic.getLessons().stream()
                        .sorted(Comparator.comparing(Lesson::getDisplayOrder))
                        .toList();
                for (Lesson lesson : lessons) {
                    LessonProgress progress = new LessonProgress();
                    progress.setUser(user);
                    progress.setLesson(lesson);
                    progress.setIsCompleted(false);
                    progress.setCompletedAt(null);
                    progress.setEnrollment(enrollment);
                    lessonProgressRepository.save(progress);
                }
            }
        }
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

    private ProgressStatus toProgressStatus(LessonProgress progress) {
        return progress.getEffectiveStatus();
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
}
