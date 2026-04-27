package org.rap.algotutorbe.quiz.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.quiz.domain.repositories.QuizAttemptRepository;
import org.rap.algotutorbe.quiz.domain.repositories.QuizRepository;
import org.rap.algotutorbe.quiz.dto.QuizResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    QuizRepository quizRepository;

    @Mock
    QuizAttemptRepository attemptRepository;

    @Mock
    LessonRepository lessonRepository;

    @Mock
    UserLessonProgressRepository lessonProgressRepository;

    @Mock
    EnrollmentRepository enrollmentRepository;

    private QuizMapper quizMapper;
    private QuizService quizService;

    private Quiz testQuiz;
    private Lesson testLesson;

    @BeforeEach
    void setUp() {
        quizMapper = new QuizMapperImpl();
        quizService = new QuizService(
                quizRepository,
                attemptRepository,
                null,
                lessonRepository,
                lessonProgressRepository,
                enrollmentRepository,
                quizMapper,
                new ObjectMapper()
        );

        LearningPath learningPath = new LearningPath();
        learningPath.setId(1L);

        testLesson = new Lesson();
        testLesson.setId(1L);
        testLesson.setTitle("Arrays Quiz Lesson");
        testLesson.setOrderIndex(1);
        testLesson.setLearningPath(learningPath);

        QuizChoice choice1a = new QuizChoice();
        choice1a.setText("A data structure");
        choice1a.setIsCorrect(true);

        QuizChoice choice1b = new QuizChoice();
        choice1b.setText("A programming language");
        choice1b.setIsCorrect(false);

        QuizQuestion question1 = new QuizQuestion();
        question1.setId(1L);
        question1.setQuestion("What is an array?");
        question1.setType(QuestionType.MULTIPLE_CHOICE);
        question1.setPoints(1);
        question1.setOrderIndex(1);
        question1.setChoices(List.of(choice1a, choice1b));

        QuizChoice choice2a = new QuizChoice();
        choice2a.setText("True");
        choice2a.setIsCorrect(true);

        QuizChoice choice2b = new QuizChoice();
        choice2b.setText("False");
        choice2b.setIsCorrect(false);

        QuizQuestion question2 = new QuizQuestion();
        question2.setId(2L);
        question2.setQuestion("Is array 0-indexed in Java?");
        question2.setType(QuestionType.TRUE_FALSE);
        question2.setPoints(1);
        question2.setOrderIndex(2);
        question2.setChoices(List.of(choice2a, choice2b));

        testQuiz = new Quiz();
        testQuiz.setId(1L);
        testQuiz.setLesson(testLesson);
        testQuiz.setTitle("Arrays Basics Quiz");
        testQuiz.setDescription("Test your array knowledge");
        testQuiz.setPassingScore(70);
        testQuiz.setTimeLimitMinutes(15);
        testQuiz.setQuestions(new ArrayList<>(List.of(question1, question2)));
    }

    @Test
    void getQuizByLessonId_returnsQuizWithQuestions() {
        when(quizRepository.findByLessonIdWithQuestions(1L))
                .thenReturn(Optional.of(testQuiz));

        ApiResponse<QuizResponse> response = quizService.getQuizByLessonId(1L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Arrays Basics Quiz", response.getData().title());
        assertEquals(2, response.getData().questionCount());
        assertEquals(70, response.getData().passingScore());
        assertEquals(15, response.getData().timeLimitMinutes());
    }

    @Test
    void getQuizByLessonId_calculatesTotalPoints() {
        when(quizRepository.findByLessonIdWithQuestions(1L))
                .thenReturn(Optional.of(testQuiz));

        ApiResponse<QuizResponse> response = quizService.getQuizByLessonId(1L);

        assertNotNull(response);
        assertEquals(2, response.getData().totalPoints());
    }
}
