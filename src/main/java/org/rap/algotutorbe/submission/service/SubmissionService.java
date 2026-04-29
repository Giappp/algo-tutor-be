package org.rap.algotutorbe.submission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
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
    private final LessonRepository lessonRepository;
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

        CodingLesson codingLesson = lessonRepository.findBySlug(request.problemSlug())
                .filter(CodingLesson.class::isInstance)
                .map(cl -> (CodingLesson) cl)
                .orElseThrow(() -> new AppException(ErrorCode.PROBLEM_NOT_FOUND));

        Submission submission = new Submission();
        submission.setLanguage(language);
        submission.setSourceCode(request.code());
        submission.setUser(user);
        submission.setCodingLesson(codingLesson);
        submission.setVerdict(Verdict.PENDING);

        Submission saved = submissionRepository.save(submission);
        log.info("Submission [{}] da luu DB, trang thai PENDING", saved.getId());

        eventPublisher.publishSubmissionCreated(new SubmissionCreatedMessage(
                saved.getId(),
                codingLesson.getId(),
                saved.getSourceCode(),
                saved.getLanguage()
        ));

        return submissionMapper.toResponse(saved);
    }

    public Submission getOrThrowSubmission(UUID submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    @Transactional
    public void markAsProcessing(UUID id) {
        Submission submission = getOrThrowSubmission(id);
        submission.setVerdict(Verdict.PROCESSING);
        log.info("Submission [{}] marked as PROCESSING", id);
    }

    @Transactional
    public void markAsSystemError(UUID id, String message) {
        Submission submission = getOrThrowSubmission(id);
        submission.setVerdict(Verdict.SYSTEM_ERROR);
        log.error("Submission [{}] marked as SYSTEM_ERROR: {}", id, message);
    }

    @Transactional
    public void updateWithJudgeResult(UUID id, Verdict verdict, Integer passedTestcases,
                                      Integer maxTime, Integer maxMemory, String compileOutput) {
        Submission submission = getOrThrowSubmission(id);
        submission.setVerdict(verdict);
        submission.setPassedTestcases(passedTestcases);
        submission.setExecutionTime(maxTime);
        submission.setMemoryUsed(maxMemory);
        submission.setCompileOutput(compileOutput);
        log.info("Submission [{}] updated with verdict={}, passed={}", id, verdict, passedTestcases);
    }

    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmissionDetail(UUID id) {
        Submission submission = submissionRepository.findByIdWithLesson(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        UUID currentUserId = getCurrentUserIdOrThrow();
        if (!submission.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.SUBMISSION_ACCESS_DENIED);
        }

        List<SubmissionTestcase> testcases = submissionTestcaseRepository.findBySubmissionId(id);
        List<SubmissionTestcaseResultResponse> testcaseResponses = submissionTestcaseMapper.toResponses(testcases);

        return submissionMapper.toDetailResponse(submission, testcaseResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> listMySubmissions(
            String lessonSlug,
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
        var submissionsPage = submissionRepository.findMySubmissions(userId, lessonSlug, verdict, lang, pageable);

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
