package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.video.VideoProgressResponse;
import org.rap.algotutorbe.learning.dto.video.VideoProgressUpdateRequest;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;
import org.rap.algotutorbe.learning.models.Enrollment;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.LessonProgress;
import org.rap.algotutorbe.learning.models.VideoLesson;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VideoProgressService extends BaseService {
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LearningAccessService learningAccessService;
    private final LessonProgressUpdater lessonProgressUpdater;

    @Value("${app.video.progress.max-watched-delta-seconds:30}")
    private int maxWatchedDeltaSeconds;

    @Value("${app.video.progress.completion-ratio:0.9}")
    private double completionRatio;

    @Transactional(readOnly = true)
    public VideoProgressResponse getProgress(String slug) {
        ProgressContext context = getProgressContext(slug);
        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentAndLesson(context.enrollment(), context.video())
                .orElse(null);

        return toResponse(context.video(), progress);
    }

    @Transactional
    public VideoProgressResponse updateProgress(String slug, VideoProgressUpdateRequest request) {
        ProgressContext context = getProgressContext(slug);
        VideoLesson video = context.video();
        validateRequest(video, request);

        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentAndLesson(context.enrollment(), video)
                .orElseGet(() -> createProgress(context));

        if (progress.getStatus() != ProgressStatus.COMPLETED) {
            int watchedSeconds = Math.min(
                    video.getDurationSeconds(),
                    progress.getVideoWatchedSeconds() + request.watchedDeltaSeconds()
            );
            progress.setVideoPositionSeconds(request.positionSeconds());
            progress.setVideoWatchedSeconds(watchedSeconds);
            progress.setVideoProgressUpdatedAt(Instant.now());

            if (hasCompletedVideo(video, watchedSeconds)) {
                progress.setStatus(ProgressStatus.COMPLETED);
                progress.setIsCompleted(true);
                progress.setCompletedAt(Instant.now());
            } else {
                progress.setStatus(ProgressStatus.IN_PROGRESS);
                progress.setIsCompleted(false);
                progress.setCompletedAt(null);
            }

            lessonProgressRepository.save(progress);
            lessonProgressUpdater.updateEnrollmentProgress(context.enrollment());
        }

        return toResponse(video, progress);
    }

    private ProgressContext getProgressContext(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if (!(lesson instanceof VideoLesson video)) {
            throw new AppException(ErrorCode.VIDEO_LESSON_REQUIRED);
        }
        if (video.getProcessingStatus() != VideoProcessingStatus.READY
                || video.getDurationSeconds() == null
                || video.getDurationSeconds() <= 0) {
            throw new AppException(ErrorCode.VIDEO_NOT_READY);
        }

        SecurityUser currentUser = getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
        if (video.getTopic() == null || video.getTopic().getLearningPath() == null) {
            throw new AppException(ErrorCode.TOPIC_NOT_IN_LEARNING_PATH);
        }

        Enrollment enrollment = enrollmentRepository
                .findByUserAndLearningPathId(currentUser.getUser(), video.getTopic().getLearningPath().getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ENROLLED));

        if (!learningAccessService.canAccessLesson(currentUser.getId(), video.getId())) {
            throw new AppException(ErrorCode.LESSON_LOCKED);
        }

        return new ProgressContext(currentUser, enrollment, video);
    }

    private void validateRequest(VideoLesson video, VideoProgressUpdateRequest request) {
        if (request.positionSeconds() > video.getDurationSeconds()
                || request.watchedDeltaSeconds() > maxWatchedDeltaSeconds) {
            throw new AppException(ErrorCode.VIDEO_PROGRESS_INVALID);
        }
    }

    private LessonProgress createProgress(ProgressContext context) {
        LessonProgress progress = new LessonProgress();
        progress.setUser(context.currentUser().getUser());
        progress.setEnrollment(context.enrollment());
        progress.setLesson(context.video());
        return progress;
    }

    private boolean hasCompletedVideo(VideoLesson video, int watchedSeconds) {
        int requiredSeconds = (int) Math.ceil(video.getDurationSeconds() * completionRatio);
        return watchedSeconds >= requiredSeconds;
    }

    private VideoProgressResponse toResponse(VideoLesson video, LessonProgress progress) {
        int positionSeconds = progress != null ? progress.getVideoPositionSeconds() : 0;
        int watchedSeconds = progress != null ? progress.getVideoWatchedSeconds() : 0;
        ProgressStatus status = progress != null ? progress.getEffectiveStatus() : ProgressStatus.NOT_STARTED;
        double percentage = Math.min(100.0, watchedSeconds * 100.0 / video.getDurationSeconds());

        return new VideoProgressResponse(
                video.getId(),
                video.getSlug(),
                video.getDurationSeconds(),
                positionSeconds,
                watchedSeconds,
                percentage,
                status,
                status == ProgressStatus.COMPLETED,
                progress != null ? progress.getVideoProgressUpdatedAt() : null
        );
    }

    private record ProgressContext(
            SecurityUser currentUser,
            Enrollment enrollment,
            VideoLesson video
    ) {
    }
}
