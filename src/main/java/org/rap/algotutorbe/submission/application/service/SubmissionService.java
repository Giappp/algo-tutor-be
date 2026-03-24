package org.rap.algotutorbe.submission.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.submission.SubmissionCreatedMessage;
import org.rap.algotutorbe.submission.application.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.application.dto.SubmitCodeRequest;
import org.rap.algotutorbe.submission.application.publisher.SubmissionEventPublisher;
import org.rap.algotutorbe.submission.domain.model.ProgrammingLanguage;
import org.rap.algotutorbe.submission.domain.model.Submission;
import org.rap.algotutorbe.submission.domain.model.Verdict;
import org.rap.algotutorbe.submission.domain.repositories.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final SubmissionEventPublisher eventPublisher;

    @Transactional
    public SubmissionResponse submitCode(SubmitCodeRequest request) {
        ProgrammingLanguage language = ProgrammingLanguage.valueOf(request.language());

        Submission submission = Submission.create(
                request.userId(), request.problemId(), request.sourceCode(), language
        );
        submissionRepository.save(submission);
        log.info("Submission [{}] đã lưu DB, trạng thái PENDING", submission.getId());

        eventPublisher.publishSubmissionCreated(new SubmissionCreatedMessage(
                submission.getId(),
                submission.getProblemId(),
                submission.getSourceCode(),
                submission.getLanguage().name()
        ));

        return new SubmissionResponse(
                submission.getId(),
                Verdict.PENDING.name(),
                "Submission đã tiếp nhận, đang chờ chấm bài"
        );
    }

    private String decodeBase64(String base64) {
        if (base64 == null) return null;
        return new String(Base64.getDecoder().decode(base64));
    }

    private Integer parseTimeMs(String timeSeconds) {
        if (timeSeconds == null) return null;
        try {
            return (int) (Double.parseDouble(timeSeconds) * 1000);
        } catch (NumberFormatException e) {
            log.warn("Không parse được time: {}", timeSeconds);
            return null;
        }
    }

    private String decodeBase64Safe(String value) {
        if (value == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    public Submission getOrThrowSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy submission: " + submissionId));
    }
}
