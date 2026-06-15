package org.rap.algotutorbe.learning.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.SlugGenerator;
import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;
import org.rap.algotutorbe.learning.mapper.LessonMapper;
import org.rap.algotutorbe.learning.models.VideoLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceVideoTest {
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private LessonMapper lessonMapper;
    @Mock
    private SlugGenerator slugGenerator;

    @InjectMocks
    private LessonService lessonService;

    @Test
    void togglePublish_shouldRejectVideoThatIsNotReady() {
        VideoLesson video = new VideoLesson();
        video.setId(42L);
        video.setIsPublished(false);
        video.setProcessingStatus(VideoProcessingStatus.UPLOADING);
        when(lessonRepository.findById(42L)).thenReturn(Optional.of(video));

        assertThatThrownBy(() -> lessonService.togglePublish(42L))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getError()).isEqualTo(ErrorCode.VIDEO_NOT_READY));

        assertThat(video.getIsPublished()).isFalse();
    }
}
