package org.rap.algotutorbe.learning.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.quiz.QuizAttemptResponse;
import org.rap.algotutorbe.learning.dto.quiz.QuizSubmitAnswer;
import org.rap.algotutorbe.learning.mapper.QuizAttemptMapper;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.QuizAttemptRepository;
import org.rap.algotutorbe.learning.repositories.QuizLessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizQuestionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizLessonRepository quizLessonRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private QuizAttemptMapper quizAttemptMapper;
    @Mock
    private LessonProgressUpdater lessonProgressUpdater;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    @Test
    void submitQuiz_shouldSucceedWithCorrectScoreCalculation() {
        // Setup User
        User user = new User();
        user.setId(UUID.randomUUID());
        SecurityUser securityUser = mock(SecurityUser.class);
        when(securityUser.getUser()).thenReturn(user);

        // Setup QuizLesson & roadmap
        QuizLesson quizLesson = new QuizLesson();
        quizLesson.setId(1L);
        quizLesson.setSlug("test-quiz");
        quizLesson.setPassingScore(70);

        Topic topic = new Topic();
        LearningPath learningPath = new LearningPath();
        learningPath.setSlug("test-roadmap");
        topic.setLearningPath(learningPath);
        quizLesson.setTopic(topic);

        // Setup QuizQuestions and Choices
        QuizQuestion question1 = new QuizQuestion();
        question1.setId(10L);
        question1.setType(QuestionType.SINGLE_CHOICE);

        QuizChoice choice1 = new QuizChoice();
        choice1.setId("c1");
        choice1.setIsCorrect(true);
        QuizChoice choice2 = new QuizChoice();
        choice2.setId("c2");
        choice2.setIsCorrect(false);
        question1.setChoices(List.of(choice1, choice2));

        QuizQuestion question2 = new QuizQuestion();
        question2.setId(11L);
        question2.setType(QuestionType.MULTIPLE_CHOICE);

        QuizChoice choice3 = new QuizChoice();
        choice3.setId("c3");
        choice3.setIsCorrect(true);
        QuizChoice choice4 = new QuizChoice();
        choice4.setId("c4");
        choice4.setIsCorrect(true);
        QuizChoice choice5 = new QuizChoice();
        choice5.setId("c5");
        choice5.setIsCorrect(false);
        question2.setChoices(List.of(choice3, choice4, choice5));

        // Adding list of questions to quiz lesson
        quizLesson.setQuestions(List.of(question1, question2));

        when(quizLessonRepository.findBySlug("test-quiz")).thenReturn(Optional.of(quizLesson));
        when(quizQuestionRepository.findById(10L)).thenReturn(Optional.of(question1));
        when(quizQuestionRepository.findById(11L)).thenReturn(Optional.of(question2));
        when(quizAttemptRepository.getNextAttemptNumber(1L)).thenReturn(1);

        // Answers payload
        QuestionAnswer answer1 = new QuestionAnswer(10L, List.of("c1")); // Correct
        QuestionAnswer answer2 = new QuestionAnswer(11L, List.of("c3", "c4")); // Correct
        QuizSubmitAnswer payload = new QuizSubmitAnswer(List.of(answer1, answer2), Instant.now(), 60, Instant.now());

        // Stub save
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Stub mapper
        QuizAttemptResponse expectedResponse = new QuizAttemptResponse(
                UUID.randomUUID(), 100.0f, true, 2, 1, Instant.now(), List.of(), true
        );
        when(quizAttemptMapper.toResponse(any(QuizAttempt.class))).thenReturn(expectedResponse);

        // Execute
        QuizAttemptResponse result = quizAttemptService.submitQuiz("test-quiz", payload, securityUser);

        // Verify
        assertThat(result).isNotNull();
        assertThat(result.score()).isEqualTo(100.0f);

        // Let's verify that quizAttempt had correct values set before save
        verify(quizAttemptRepository).save(argThat(attempt -> {
            assertThat(attempt.getScore()).isEqualTo(100.0f);
            assertThat(attempt.getCorrectCount()).isEqualTo(2);
            assertThat(attempt.getPassed()).isTrue();
            return true;
        }));
    }

    @Test
    void submitQuiz_shouldHandleNullIsCorrectInChoices() {
        // Setup User
        User user = new User();
        user.setId(UUID.randomUUID());
        SecurityUser securityUser = mock(SecurityUser.class);
        when(securityUser.getUser()).thenReturn(user);

        // Setup QuizLesson & roadmap
        QuizLesson quizLesson = new QuizLesson();
        quizLesson.setId(1L);
        quizLesson.setSlug("test-quiz");
        quizLesson.setPassingScore(70);

        Topic topic = new Topic();
        LearningPath learningPath = new LearningPath();
        learningPath.setSlug("test-roadmap");
        topic.setLearningPath(learningPath);
        quizLesson.setTopic(topic);

        // Setup QuizQuestion with choice having null isCorrect
        QuizQuestion question1 = new QuizQuestion();
        question1.setId(10L);
        question1.setType(QuestionType.SINGLE_CHOICE);

        QuizChoice choice1 = new QuizChoice();
        choice1.setId("c1");
        choice1.setIsCorrect(null); // Null value
        QuizChoice choice2 = new QuizChoice();
        choice2.setId("c2");
        choice2.setIsCorrect(true);
        question1.setChoices(List.of(choice1, choice2));

        quizLesson.setQuestions(List.of(question1));

        when(quizLessonRepository.findBySlug("test-quiz")).thenReturn(Optional.of(quizLesson));
        when(quizQuestionRepository.findById(10L)).thenReturn(Optional.of(question1));
        when(quizAttemptRepository.getNextAttemptNumber(1L)).thenReturn(1);

        // Answers payload
        QuestionAnswer answer1 = new QuestionAnswer(10L, List.of("c2"));
        QuizSubmitAnswer payload = new QuizSubmitAnswer(List.of(answer1), Instant.now(), 60, Instant.now());

        // Stub save
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        quizAttemptService.submitQuiz("test-quiz", payload, securityUser);

        // Verify it passes without NPE
        verify(quizAttemptRepository).save(any(QuizAttempt.class));
    }
}
