package org.rap.algotutorbe.ai.services;

import org.rap.algotutorbe.ai.dto.CodingAiGenerationResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.ProblemExample;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CodingDraftValidator {

    private static final Pattern SIGNATURE_FUNCTION =
            Pattern.compile("(?i)function\\s*:\\s*([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern CODE_SIGNATURE =
            Pattern.compile("(?im)^\\s*(?://|#)\\s*Signature:\\s*(.+)$");
    private static final Pattern SOLUTION_MARKERS =
            Pattern.compile("(?i)(return\\s+[^;\\n]+|TODO[^\\n]*(implemented|solution)|complete solution)");

    public void validateProblem(
            CodingAiGenerationResponse.ProblemContent content,
            int exampleCount,
            int hintCount) {
        if (content == null
                || isBlank(content.statement())
                || content.constraints() == null
                || content.constraints().stream().anyMatch(this::isBlank)
                || content.examples() == null
                || content.examples().size() != exampleCount
                || content.hints() == null
                || content.hints().size() != hintCount
                || content.hints().stream().anyMatch(this::isBlank)) {
            invalidDraft();
        }
        for (ProblemExample example : content.examples()) {
            if (example == null || isBlank(example.input()) || isBlank(example.output())) {
                invalidDraft();
            }
        }
    }

    public void validateEditorial(CodingAiGenerationResponse.EditorialContent content, ProgrammingLanguage language) {
        if (content == null
                || content.language() != language
                || isBlank(content.sourceCode())
                || isBlank(content.approachSummary())
                || isBlank(content.timeComplexity())
                || isBlank(content.spaceComplexity())) {
            invalidDraft();
        }
    }

    public void validateStarter(
            CodingAiGenerationResponse.StarterCodeContent content,
            Set<ProgrammingLanguage> languages) {
        if (content == null || content.starterCode() == null || isBlank(content.signatureSummary())) {
            invalidDraft();
        }

        Set<String> expectedKeys = languages.stream()
                .map(language -> language.name().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (!content.starterCode().keySet().equals(expectedKeys)) {
            invalidDraft();
        }

        Matcher matcher = SIGNATURE_FUNCTION.matcher(content.signatureSummary());
        if (!matcher.find()) {
            invalidDraft();
        }
        String functionName = matcher.group(1);
        String expectedSignature = null;
        for (Map.Entry<String, String> entry : content.starterCode().entrySet()) {
            if (isBlank(entry.getValue())) {
                invalidDraft();
            }
            Matcher signatureMatcher = CODE_SIGNATURE.matcher(entry.getValue());
            if (!signatureMatcher.find()
                    || !Pattern.compile("\\b" + Pattern.quote(functionName) + "\\b").matcher(entry.getValue()).find()
                    || SOLUTION_MARKERS.matcher(entry.getValue()).find()) {
                invalidDraft();
            }
            String signature = normalizeSignature(signatureMatcher.group(1));
            if (expectedSignature == null) {
                expectedSignature = signature;
            } else if (!expectedSignature.equals(signature)) {
                invalidDraft();
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeSignature(String signature) {
        return signature.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private void invalidDraft() {
        throw new AppException(ErrorCode.INVALID_AI_CODING_DRAFT);
    }
}
