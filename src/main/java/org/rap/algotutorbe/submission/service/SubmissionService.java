package org.rap.algotutorbe.submission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.submission.dto.SubmissionDetailResponse;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.dto.SubmissionTestcaseResultResponse;
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

/**
 * Service for querying submission history and details.
 * Submission creation is handled by JudgeService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService extends BaseService {
    private final SubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;
    private final SubmissionTestcaseMapper submissionTestcaseMapper;
    private final SubmissionTestcaseRepository submissionTestcaseRepository;

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
        ProgrammingLanguage lang = language != null && !language.isBlank()
                ? ProgrammingLanguage.fromApiValue(language.trim()) : null;

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
