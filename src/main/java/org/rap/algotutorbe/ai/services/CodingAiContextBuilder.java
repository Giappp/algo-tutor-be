package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.ai.dto.AiQuestionSourceResponse;
import org.rap.algotutorbe.ai.dto.CodingAiGenerationResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CodingAiContextBuilder {

    private static final int CONTENT_BUDGET = 30_000;
    private static final int PREVIEW_LENGTH = 180;
    private static final Pattern SCRIPT_OR_STYLE =
            Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
    private static final Pattern HTML_TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final LessonRepository lessonRepository;

    public CodingLesson getCodingLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if (!(lesson instanceof CodingLesson codingLesson)) {
            throw new AppException(ErrorCode.CODING_LESSON_REQUIRED);
        }
        return codingLesson;
    }

    public List<AiQuestionSourceResponse> getSources(CodingLesson lesson) {
        return availableSources(lesson).stream().map(this::toSourceResponse).toList();
    }

    public BuiltContext build(CodingLesson lesson, List<Long> sourceIds) {
        if (new HashSet<>(sourceIds).size() != sourceIds.size()) {
            throw new AppException(ErrorCode.INVALID_AI_CODING_SOURCES);
        }

        Map<Long, TheoryLesson> available = new LinkedHashMap<>();
        availableSources(lesson).forEach(source -> available.put(source.getId(), source));
        if (!available.keySet().containsAll(sourceIds)) {
            throw new AppException(ErrorCode.INVALID_AI_CODING_SOURCES);
        }

        List<TheoryLesson> selected = available.values().stream()
                .filter(source -> sourceIds.contains(source.getId()))
                .toList();
        int perSourceBudget = selected.isEmpty() ? CONTENT_BUDGET : CONTENT_BUDGET / selected.size();
        List<Long> truncated = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        for (TheoryLesson source : selected) {
            String cleaned = clean(source.getContent());
            if (cleaned.length() > perSourceBudget) {
                cleaned = cleaned.substring(0, perSourceBudget);
                truncated.add(source.getId());
            }
            content.append("\n[SOURCE lessonId=").append(source.getId())
                    .append(" title=\"").append(source.getTitle()).append("\" topic=\"")
                    .append(source.getTopic().getName()).append("\"]\n")
                    .append(cleaned).append("\n[/SOURCE]\n");
        }

        return new BuiltContext(
                content.toString(),
                new CodingAiGenerationResponse.GenerationContext(
                        selected.stream().map(this::toContextSource).toList(),
                        truncated));
    }

    private List<TheoryLesson> availableSources(CodingLesson lesson) {
        return lessonRepository.findQuestionSourcesByLearningPathId(
                lesson.getTopic().getLearningPath().getId());
    }

    private AiQuestionSourceResponse toSourceResponse(TheoryLesson source) {
        String cleaned = clean(source.getContent());
        return new AiQuestionSourceResponse(
                source.getId(), source.getTitle(), source.getTopic().getId(), source.getTopic().getName(),
                source.getDisplayOrder(), source.getEstimatedMinutes(), source.getContent().length(),
                cleaned.substring(0, Math.min(PREVIEW_LENGTH, cleaned.length())), source.getIsPublished());
    }

    private CodingAiGenerationResponse.Source toContextSource(TheoryLesson source) {
        return new CodingAiGenerationResponse.Source(
                source.getId(), source.getTitle(), source.getTopic().getName(), source.getIsPublished());
    }

    private String clean(String value) {
        String withoutDangerousBlocks = SCRIPT_OR_STYLE.matcher(value == null ? "" : value).replaceAll(" ");
        return WHITESPACE.matcher(HTML_TAG.matcher(withoutDangerousBlocks).replaceAll(" ")).replaceAll(" ").trim();
    }

    public record BuiltContext(String sourceContent, CodingAiGenerationResponse.GenerationContext responseContext) {
    }
}
