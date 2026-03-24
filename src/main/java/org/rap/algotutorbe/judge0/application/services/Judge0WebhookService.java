package org.rap.algotutorbe.judge0.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge0.application.dto.Judge0WebhookPayload;
import org.rap.algotutorbe.submission.domain.model.SubmissionTestcase;
import org.rap.algotutorbe.submission.domain.model.Verdict;
import org.rap.algotutorbe.submission.domain.repositories.SubmissionRepository;
import org.rap.algotutorbe.submission.domain.repositories.SubmissionTestcaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class Judge0WebhookService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionTestcaseRepository submissionTestcaseRepository;

    @Transactional
    public void processResult(Long submissionId, Integer testcaseIndex, Judge0WebhookPayload payload) {
        SubmissionTestcase submissionTestcase = submissionTestcaseRepository
                .findBySubmissionIdAndTestcaseIndex(submissionId, testcaseIndex)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy testcase"));

        if (submissionTestcase.getVerdict() != Verdict.PENDING) {
            return;
        }

        submissionTestcase.setVerdict(mapJudge0Status(payload.status().id()));
        submissionTestcase.setTime(payload.time());
        submissionTestcase.setMemory(payload.memory());
        submissionTestcase.setCompileOutput(decodeBase64(payload.compileOutput()));
        // Bạn có thể lưu thêm stdout/stderr nếu muốn hiển thị cho người dùng debug
        // testcase.setStdout(decodeBase64(payload.stdout()));

        submissionTestcaseRepository.save(submissionTestcase);
    }

    private Verdict mapJudge0Status(int statusId) {
        return switch (statusId) {
            case 1, 2 -> Verdict.PENDING;
            case 3 -> Verdict.ACCEPTED;
            case 4 -> Verdict.WRONG_ANSWER;
            case 5 -> Verdict.TIME_LIMIT_EXCEEDED;
            case 6 -> Verdict.COMPILATION_ERROR;
            case 7 -> Verdict.SIGSEGV;
            case 8 -> Verdict.SIGXFSZ;
            case 9 -> Verdict.SIGFPE;
            case 10 -> Verdict.SIGABRT;
            case 11 -> Verdict.RUNTIME_ERROR;
            default -> Verdict.INTERNAL_ERROR;
        };
    }

    private String decodeBase64(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.error("Lỗi decode base64: {}", base64);
            return null;
        }
    }
}