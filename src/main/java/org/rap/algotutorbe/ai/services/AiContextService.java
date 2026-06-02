package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiContextService {
    private final LessonRepository lessonRepository;

    public String buildContext(AiChatRequest request) {
        StringBuilder context = new StringBuilder();
        if (request.lessonSlug() != null) {
            lessonRepository.findBySlug(request.lessonSlug()).ifPresent(lesson -> buildLesson(lesson, context));
        }
        if (request.judgeResult() != null) {
            context.append("""
                    [JUDGE_RESULT]
                    %s
                    [/JUDGE_RESULT]
                    
                    """.formatted(request.judgeResult()));
        }

        if (request.errorMessage() != null) {
            context.append("""
                    [ERROR_MESSAGE]
                    %s
                    [/ERROR_MESSAGE]
                    
                    """.formatted(request.errorMessage()));
        }

        if (request.failedTestCases() != null && !request.failedTestCases().isEmpty()) {
            context.append("[FAILED_TEST_CASES]\n");
            for (int i = 0; i < request.failedTestCases().size(); i++) {
                context.append("Test Case %d: %s%n".formatted(i + 1, request.failedTestCases().get(i)));
            }
            context.append("[/FAILED_TEST_CASES]\n\n");
        }

        return context.toString();
    }

    private void buildLesson(Lesson lesson, StringBuilder context) {
        if (lesson instanceof TheoryLesson theoryLesson) {
            context.append("""
                    [LESSON_CONTEXT]
                    Title: %s
                    Difficulty: %s
                    Content:
                    %s
                    [/LESSON_CONTEXT]
                    
                    """.formatted(
                    theoryLesson.getTitle(),
                    theoryLesson.getDifficulty(),
                    theoryLesson.getContent()
            ));
        } else if (lesson instanceof CodingLesson codingLesson) {
            context.append("""
                    [LESSON_CONTEXT]
                    Title: %s
                    Difficulty: %s
                    Statement:
                    %s
                    Constraints:
                    %s
                    [/LESSON_CONTEXT]
                    """.formatted(
                    codingLesson.getTitle(),
                    codingLesson.getDifficulty(),
                    codingLesson.getStatement(),
                    codingLesson.getConstraints()));
        }
    }
}
