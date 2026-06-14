package org.rap.algotutorbe.ai.services;

import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.CodingLessonRequestDTO;
import org.rap.algotutorbe.learning.dto.LessonRequestDTO;
import org.rap.algotutorbe.learning.dto.QuizChoiceDTO;
import org.rap.algotutorbe.learning.dto.QuizQuestionDTO;
import org.rap.algotutorbe.learning.dto.TheoryLessonRequestDTO;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.QuestionType;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownLessonRequestBuilder {

    private static final String QUESTION_HEADING_PREFIX = "## Question";
    private static final String CORRECT_CHOICE_PREFIX = "- [x] ";
    private static final String CORRECT_CHOICE_UPPERCASE_PREFIX = "- [X] ";
    private static final String INCORRECT_CHOICE_PREFIX = "- [ ] ";
    private static final String EXPLANATION_PREFIX = "> Explanation:";

    public Object build(Lesson lesson, String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new AppException(ErrorCode.INVALID_AI_GENERATED_CONTENT);
        }

        Object content = switch (lesson) {
            case TheoryLesson theory -> buildTheory(theory, markdown);
            case CodingLesson coding -> buildCoding(coding, markdown);
            case QuizLesson ignored -> parseQuizQuestions(markdown);
            default -> throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        };

        if (content instanceof LessonRequestDTO request) {
            request.setTitle(lesson.getTitle());
            request.setType(lesson.getType());
            request.setDifficulty(lesson.getDifficulty());
            request.setDisplayOrder(lesson.getDisplayOrder());
        }
        return content;
    }

    private TheoryLessonRequestDTO buildTheory(TheoryLesson lesson, String markdown) {
        TheoryLessonRequestDTO request = new TheoryLessonRequestDTO();
        request.setContent(markdown.trim());
        request.setEstimatedMinutes(lesson.getEstimatedMinutes());
        return request;
    }

    private CodingLessonRequestDTO buildCoding(CodingLesson lesson, String markdown) {
        CodingLessonRequestDTO request = new CodingLessonRequestDTO();
        request.setStatement(markdown.trim());
        request.setBaseTimeLimitMs(lesson.getBaseTimeLimitMs());
        request.setBaseMemoryLimitMb(lesson.getBaseMemoryLimitMb());
        request.setConstraints(copy(lesson.getConstraints()));
        request.setStarterCode(lesson.getStarterCode() == null ? null : new java.util.LinkedHashMap<>(lesson.getStarterCode()));
        request.setExamples(copy(lesson.getExamples()));
        request.setHints(copy(lesson.getHints()));
        return request;
    }

    private List<QuizQuestionDTO> parseQuizQuestions(String markdown) {
        List<List<String>> sections = splitQuestionSections(markdown);
        List<QuizQuestionDTO> questions = new ArrayList<>();

        for (int index = 0; index < sections.size(); index++) {
            questions.add(parseQuestion(sections.get(index), index + 1));
        }

        if (questions.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_AI_GENERATED_CONTENT);
        }
        return questions;
    }

    private List<List<String>> splitQuestionSections(String markdown) {
        List<List<String>> sections = new ArrayList<>();
        List<String> current = null;

        for (String rawLine : markdown.replace("\r\n", "\n").split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith(QUESTION_HEADING_PREFIX)) {
                if (current != null) {
                    sections.add(current);
                }
                current = new ArrayList<>();
                continue;
            }
            if (current != null) {
                current.add(line);
            }
        }

        if (current != null) {
            sections.add(current);
        }
        return sections;
    }

    private QuizQuestionDTO parseQuestion(List<String> lines, int orderIndex) {
        StringBuilder questionText = new StringBuilder();
        String explanation = null;
        List<QuizChoiceDTO> choices = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith(CORRECT_CHOICE_PREFIX)) {
                choices.add(choice(choices.size(), line.substring(CORRECT_CHOICE_PREFIX.length()), true));
            } else if (line.startsWith(CORRECT_CHOICE_UPPERCASE_PREFIX)) {
                choices.add(choice(choices.size(), line.substring(CORRECT_CHOICE_UPPERCASE_PREFIX.length()), true));
            } else if (line.startsWith(INCORRECT_CHOICE_PREFIX)) {
                choices.add(choice(choices.size(), line.substring(INCORRECT_CHOICE_PREFIX.length()), false));
            } else if (line.startsWith(EXPLANATION_PREFIX)) {
                explanation = line.substring(EXPLANATION_PREFIX.length()).trim();
            } else if (!line.isBlank() && choices.isEmpty()) {
                if (!questionText.isEmpty()) {
                    questionText.append(' ');
                }
                questionText.append(line);
            }
        }

        long correctChoices = choices.stream().filter(choice -> Boolean.TRUE.equals(choice.isCorrect())).count();
        if (questionText.isEmpty() || choices.size() < 2 || correctChoices == 0) {
            throw new AppException(ErrorCode.INVALID_AI_GENERATED_CONTENT);
        }

        QuestionType type = correctChoices > 1 ? QuestionType.MULTIPLE_CHOICE : QuestionType.SINGLE_CHOICE;
        return new QuizQuestionDTO(questionText.toString(), type, 1, explanation, orderIndex, choices);
    }

    private QuizChoiceDTO choice(int index, String text, boolean correct) {
        if (text.isBlank()) {
            throw new AppException(ErrorCode.INVALID_AI_GENERATED_CONTENT);
        }
        return new QuizChoiceDTO(String.valueOf((char) ('a' + index)), text.trim(), correct, null);
    }

    private <T> List<T> copy(List<T> source) {
        return source == null ? null : new ArrayList<>(source);
    }
}
