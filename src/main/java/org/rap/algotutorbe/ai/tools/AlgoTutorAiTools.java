package org.rap.algotutorbe.ai.tools;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.ai.dto.ProblemToolResult;
import org.rap.algotutorbe.ai.dto.SubmissionToolResult;
import org.rap.algotutorbe.learning.repositories.CodingLessonRepository;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlgoTutorAiTools {

    private final CodingLessonRepository codingLessonRepository;
    private final SubmissionRepository submissionRepository;

    @Tool(description = "Get coding lesson information by lesson ID")
    public ProblemToolResult getCodingLesson(Long codingLessonId) {
        return codingLessonRepository.findById(codingLessonId)
                .map(codingLesson -> new ProblemToolResult(
                        codingLesson.getId(),
                        codingLesson.getTitle(),
                        codingLesson.getDifficulty().toApiValue(),
                        codingLesson.getStatement(),
                        codingLesson.getConstraints()
                ))
                .orElse(new ProblemToolResult(
                        null,
                        "Not Found",
                        null,
                        "Coding lesson with ID " + codingLessonId + " does not exist.",
                        Collections.emptyList()
                ));
    }

    @Tool(description = "Get user's latest submission for a lesson")
    public SubmissionToolResult getLatestSubmission(Long codingLessonId) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof org.rap.algotutorbe.iam.infrastructure.SecurityUser securityUser)) {
            return new SubmissionToolResult(
                    null,
                    "No Authentication",
                    null,
                    "User is not authenticated."
            );
        }
        UUID userId = securityUser.getId();
        return submissionRepository
                .findTopByUserIdAndCodingLessonIdOrderByCreatedAtDesc(userId, codingLessonId)
                .map(submission -> new SubmissionToolResult(
                        submission.getId(),
                        submission.getVerdict().name(),
                        submission.getLanguage().name(),
                        submission.getSourceCode()
                ))
                .orElse(new SubmissionToolResult(
                        null,
                        "No Submission",
                        null,
                        "No submission found for user " + userId + " and coding lesson " + codingLessonId + "."
                ));
    }
}