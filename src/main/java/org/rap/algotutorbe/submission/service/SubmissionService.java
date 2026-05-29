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
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.rap.algotutorbe.submission.mapper.SubmissionMapper;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
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

    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmissionDetail(UUID id) {
        Submission submission = submissionRepository.findByIdWithLesson(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        UUID currentUserId = getCurrentUserIdOrThrow();
        if (!submission.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.SUBMISSION_ACCESS_DENIED);
        }

        return submissionMapper.toDetailResponse(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> listMySubmissions(
            String lessonSlug,
            String status,
            String language,
            Integer page,
            Integer limit) {
        UUID userId = getCurrentUserIdOrThrow();
        int pageNum = page == null || page < 1 ? 1 : page;
        int size = limit == null ? 20 : Math.clamp(limit, 1, 100);

        Verdict verdict = null;
        if (status != null && !status.isBlank()) {
            try {
                verdict = Verdict.fromApiValue(status.trim());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_PROGRESS_STATUS);
            }
        }

        ProgrammingLanguage lang = null;
        if (language != null && !language.isBlank()) {
            try {
                lang = ProgrammingLanguage.fromApiValue(language.trim());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.UNSUPPORTED_PROGRAMMING_LANGUAGE);
            }
        }

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
