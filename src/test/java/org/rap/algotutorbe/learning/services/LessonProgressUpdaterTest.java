package org.rap.algotutorbe.learning.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.learning.enums.EnrollmentStatus;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Enrollment;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.LessonProgress;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonProgressRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonProgressUpdaterTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LearningAccessService learningAccessService;

    private LessonProgressUpdater updater;
    private User user;
    private CodingLesson lesson;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        updater = new LessonProgressUpdater(
                enrollmentRepository,
                lessonProgressRepository,
                lessonRepository,
                learningAccessService
        );

        user = new User();
        user.setId(UUID.randomUUID());

        LearningPath learningPath = new LearningPath();
        learningPath.setId(10L);

        Topic topic = new Topic();
        topic.setLearningPath(learningPath);

        lesson = new CodingLesson();
        lesson.setId(20L);
        lesson.setSlug("two-sum");
        lesson.setTopic(topic);

        enrollment = new Enrollment();
        enrollment.setId(UUID.randomUUID());
        enrollment.setUser(user);
        enrollment.setLearningPath(learningPath);
        enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
    }

    @Test
    void markLessonCompleted_returnsTrueWhenProgressChanges() {
        when(enrollmentRepository.findByUserAndLearningPathIdAndStatus(
                user,
                10L,
                EnrollmentStatus.IN_PROGRESS
        )).thenReturn(Optional.of(enrollment));
        when(learningAccessService.canAccessLesson(user.getId(), lesson.getId())).thenReturn(true);
        when(lessonProgressRepository.findByEnrollmentAndLesson(enrollment, lesson)).thenReturn(Optional.empty());
        when(lessonRepository.countPublishedLessonsByLearningPathId(10L)).thenReturn(1L);
        when(lessonProgressRepository.countCompletedByEnrollmentId(enrollment.getId())).thenReturn(1L);

        boolean updated = updater.markLessonCompleted(user, lesson);

        assertThat(updated).isTrue();
        verify(lessonProgressRepository).save(any(LessonProgress.class));
        verify(enrollmentRepository).save(enrollment);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void markLessonCompleted_returnsFalseWhenLessonAlreadyCompleted() {
        LessonProgress existingProgress = new LessonProgress();
        existingProgress.setStatus(ProgressStatus.COMPLETED);

        when(enrollmentRepository.findByUserAndLearningPathIdAndStatus(
                user,
                10L,
                EnrollmentStatus.IN_PROGRESS
        )).thenReturn(Optional.of(enrollment));
        when(learningAccessService.canAccessLesson(user.getId(), lesson.getId())).thenReturn(true);
        when(lessonProgressRepository.findByEnrollmentAndLesson(enrollment, lesson))
                .thenReturn(Optional.of(existingProgress));

        boolean updated = updater.markLessonCompleted(user, lesson);

        assertThat(updated).isFalse();
        verify(lessonProgressRepository, never()).save(any(LessonProgress.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }
}
