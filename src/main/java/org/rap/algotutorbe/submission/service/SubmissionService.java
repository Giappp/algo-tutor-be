package org.rap.algotutorbe.submission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.rap.algotutorbe.problem.repositories.ProblemRepository;
import org.rap.algotutorbe.submission.dto.*;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.SubmissionTestcase;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.rap.algotutorbe.submission.mapper.SubmissionMapper;
import org.rap.algotutorbe.submission.mapper.SubmissionTestcaseMapper;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
import org.rap.algotutorbe.submission.repositories.SubmissionTestcaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService extends BaseService {
    private final SubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;
    private final SubmissionTestcaseMapper submissionTestcaseMapper;
    private final SubmissionEventPublisher eventPublisher;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SubmissionTestcaseRepository submissionTestcaseRepository;

    @Transactional
    public SubmissionResponse submitCode(SubmitCodeRequest request) {
        ProgrammingLanguage language;
        try {
            language = ProgrammingLanguage.fromApiValue(request.language());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNSUPPORTED_PROGRAMMING_LANGUAGE);
        }

        User user = userRepository.findById(getCurrentUserIdOrThrow())
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        Submission submission = new Submission();
        submission.setLanguage(language);
        submission.setSourceCode(request.code());
        submission.setUser(user);
        submission.setVerdict(Verdict.PENDING);

        submissionRepository.save(submission);
        log.info("Submission [{}] da luu DB, trang thai PENDING", submission.getId());


        eventPublisher.publishSubmissionCreated(new SubmissionCreatedMessage(
                submission.getId(),
                submission.getProblem().getId(),
                submission.getSourceCode(),
                submission.getLanguage()
        ));

        return submissionMapper.toResponse(submission);
    }

    public Submission getOrThrowSubmission(UUID submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    @Transactional
    public void markAsProcessing(UUID id) {
        Submission submission = getOrThrowSubmission(id);
        submission.setVerdict(Verdict.PROCESSING);
        submissionRepository.save(submission);
        log.info("Submission [{}] marked as PROCESSING", id);
    }

    @Transactional
    public void markAsSystemError(UUID id, String message) {
        Submission submission = getOrThrowSubmission(id);
        submission.setVerdict(Verdict.SYSTEM_ERROR);
        submissionRepository.save(submission);
        log.error("Submission [{}] marked as SYSTEM_ERROR: {}", id, message);
    }

    @Transactional
    public void updateWithJudgeResult(UUID id, Verdict verdict, Integer passedTestcases,
                                      Integer maxTime, Integer maxMemory, String compileOutput) {
        Submission submission = getOrThrowSubmission(id);
        submission.setVerdict(verdict);
        submission.setPassedTestcases(passedTestcases);
        submission.setMaxTime(maxTime);
        submission.setMaxMemory(maxMemory);
        submission.setCompileOutput(compileOutput);
        submissionRepository.save(submission);
        log.info("Submission [{}] updated with verdict={}, passed={}", id, verdict, passedTestcases);
    }

    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmissionDetail(UUID id) {
        Submission submission = submissionRepository.findByIdWithProblem(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        List<SubmissionTestcase> testcases = submissionTestcaseRepository.findBySubmissionId(id);
        List<SubmissionTestcaseResultResponse> testcaseResponses = submissionTestcaseMapper.toResponses(testcases);

        return submissionMapper.toDetailResponse(submission, testcaseResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> listMySubmissions(
            String problemSlug,
            String status,
            String language,
            Integer page,
            Integer limit
    ) {
        UUID userId = getCurrentUserIdOrThrow();
        int pageNum = page == null || page < 1 ? 1 : page;
        int size = limit == null ? 20 : Math.clamp(limit, 1, 100);

        Verdict verdict = status != null && !status.isBlank() ? Verdict.fromApiValue(status.trim()) : null;
        ProgrammingLanguage lang = language != null && !language.isBlank() ? ProgrammingLanguage.fromApiValue(language.trim()) : null;

        var pageable = org.springframework.data.domain.PageRequest.of(pageNum - 1, size);
        var submissionsPage = submissionRepository.findMySubmissions(userId, problemSlug, verdict, lang, pageable);

        List<SubmissionResponse> data = submissionsPage.getContent().stream()
                .map(submissionMapper::toResponse)
                .toList();

        return PageResponse.<SubmissionResponse>builder()
                .data(data)
                .currentPage(pageNum)
                .totalElements(submissionsPage.getTotalElements())
                .totalPages(submissionsPage.getTotalPages())
                .pageSize(size)
                .build();
    }
}
