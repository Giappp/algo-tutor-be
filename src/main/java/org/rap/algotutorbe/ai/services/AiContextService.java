package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiContextService {
    private static final int MAX_JUDGE_RESULT_LENGTH = 2_000;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2_000;
    private static final int MAX_FAILED_TEST_CASES = 5;
    private static final int MAX_FAILED_TEST_CASE_LENGTH = 1_000;

    private final LessonRepository lessonRepository;

    public String buildContext(AiChatRequest request) {
        StringBuilder context = new StringBuilder();
        resolveLesson(request).ifPresent(lesson -> buildLesson(lesson, context));

        if (request.judgeResult() != null) {
            context.append("""
                    [JUDGE_RESULT]
                    %s
                    [/JUDGE_RESULT]
                    
                    """.formatted(truncate(request.judgeResult(), MAX_JUDGE_RESULT_LENGTH)));
        }

        if (request.errorMessage() != null) {
            context.append("""
                    [ERROR_MESSAGE]
                    %s
                    [/ERROR_MESSAGE]
                    
                    """.formatted(truncate(request.errorMessage(), MAX_ERROR_MESSAGE_LENGTH)));
        }

        if (request.failedTestCases() != null && !request.failedTestCases().isEmpty()) {
            context.append("[FAILED_TEST_CASES]\n");
            int limit = Math.min(MAX_FAILED_TEST_CASES, request.failedTestCases().size());
            for (int i = 0; i < limit; i++) {
                context.append("Testcase %d: %s%n".formatted(
                        i + 1,
                        truncate(request.failedTestCases().get(i), MAX_FAILED_TEST_CASE_LENGTH)));
            }
            if (request.failedTestCases().size() > limit) {
                context.append("... đã lược bớt %d testcase lỗi.\n".formatted(
                        request.failedTestCases().size() - limit));
            }
            context.append("[/FAILED_TEST_CASES]\n\n");
        }

        return context.toString();
    }

    private Optional<Lesson> resolveLesson(AiChatRequest request) {
        if (request.lessonId() != null) {
            return lessonRepository.findById(request.lessonId());
        }
        if (request.lessonSlug() != null && !request.lessonSlug().isBlank()) {
            return lessonRepository.findBySlug(request.lessonSlug());
        }
        return Optional.empty();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n...[đã rút gọn]";
    }

    private void buildLesson(Lesson lesson, StringBuilder context) {
        if (lesson instanceof TheoryLesson theoryLesson) {
            context.append("""
                    [LESSON_CONTEXT]
                    Tiêu đề: %s
                    Độ khó: %s
                    Nội dung:
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
                    Tiêu đề: %s
                    Độ khó: %s
                    Đề bài:
                    %s
                    Ràng buộc:
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
