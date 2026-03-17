package org.rap.algotutorbe.submission.internal.application.service;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.submission.internal.application.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.internal.application.dto.SubmitCodeRequest;
import org.rap.algotutorbe.submission.internal.domain.model.ProgrammingLanguage;
import org.rap.algotutorbe.submission.internal.domain.model.Submission;
import org.rap.algotutorbe.submission.internal.domain.repositories.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final SubmissionRepository repository;

    @Transactional
    public SubmissionResponse submitCode(Long currentUserId, SubmitCodeRequest request) {
        Submission submission = Submission.create(
                currentUserId,
                request.problemId(),
                request.sourceCode(),
                ProgrammingLanguage.valueOf(request.language().toUpperCase())
        );

        repository.save(submission);

        return new SubmissionResponse(
                submission.getId(),
                submission.getVerdict().name(),
                submission.getLanguage().name(),
                submission.getSubmittedAt().toString()
        );
    }
}
