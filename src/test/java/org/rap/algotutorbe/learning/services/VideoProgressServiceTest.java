package org.rap.algotutorbe.learning.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.video.VideoProgressUpdateRequest;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;
import org.rap.algotutorbe.learning.models.Enrollment;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.LessonProgress;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.models.VideoLesson;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoProgressServiceTest {
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private LearningAccessService learningAccessService;
    @Mock
    private LessonProgressUpdater lessonProgressUpdater;

    private VideoProgressService videoProgressService;
    private VideoLesson video;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        videoProgressService = new VideoProgressService(
                lessonRepository,
                enrollmentRepository,
                lessonProgressRepository,
                learningAccessService,
                lessonProgressUpdater
        );
        ReflectionTestUtils.setField(videoProgressService, "maxWatchedDeltaSeconds", 30);
        ReflectionTestUtils.setField(videoProgressService, "completionRatio", 0.9);

        User user = new User();
        user.setId(UUID.randomUUID());
        SecurityUser securityUser = org.mockito.Mockito.mock(SecurityUser.class);
        when(securityUser.getUser()).thenReturn(user);
        when(securityUser.getId()).thenReturn(user.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(securityUser, null, List.of())
        );

        LearningPath learningPath = new LearningPath();
        learningPath.setId(7L);
        Topic topic = new Topic();
        topic.setLearningPath(learningPath);

        video = new VideoLesson();
        video.setId(42L);
        video.setSlug("binary-search-video");
        video.setTopic(topic);
        video.setDurationSeconds(100);
        video.setProcessingStatus(VideoProcessingStatus.READY);

        enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setLearningPath(learningPath);

        when(lessonRepository.findBySlug(video.getSlug())).thenReturn(Optional.of(video));
        when(enrollmentRepository.findByUserAndLearningPathId(user, 7L)).thenReturn(Optional.of(enrollment));
        when(learningAccessService.canAccessLesson(user.getId(), video.getId())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProgress_shouldReturnNotStartedWhenNoProgressExists() {
        when(lessonProgressRepository.findByEnrollmentAndLesson(enrollment, video)).thenReturn(Optional.empty());

        var response = videoProgressService.getProgress(video.getSlug());

        assertThat(response.positionSeconds()).isZero();
        assertThat(response.watchedSeconds()).isZero();
        assertThat(response.status()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(response.completed()).isFalse();
    }

    @Test
    void updateProgress_shouldCompleteAtConfiguredThreshold() {
        LessonProgress progress = new LessonProgress();
        progress.setUser(enrollment.getUser());
        progress.setEnrollment(enrollment);
        progress.setLesson(video);
        progress.setVideoWatchedSeconds(70);
        when(lessonProgressRepository.findByEnrollmentAndLesson(enrollment, video))
                .thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any(LessonProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = videoProgressService.updateProgress(
                video.getSlug(),
                new VideoProgressUpdateRequest(90, 20)
        );

        assertThat(response.watchedSeconds()).isEqualTo(90);
        assertThat(response.status()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(response.completed()).isTrue();
        verify(lessonProgressUpdater).updateEnrollmentProgress(enrollment);
    }

    @Test
    void updateProgress_shouldRejectDeltaAboveLimit() {
        assertThatThrownBy(() -> videoProgressService.updateProgress(
                video.getSlug(),
                new VideoProgressUpdateRequest(40, 31)
        ))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getError()).isEqualTo(ErrorCode.VIDEO_PROGRESS_INVALID));
    }
}
