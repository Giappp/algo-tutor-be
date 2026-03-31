package org.rap.algotutorbe.judge.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.application.dto.JudgeResult;
import org.rap.algotutorbe.judge.application.dto.PistonResponse;
import org.rap.algotutorbe.problem.application.dto.TestcaseDto;
import org.rap.algotutorbe.problem.application.services.ProblemService;
import org.rap.algotutorbe.submission.SubmissionCreatedMessage;
import org.rap.algotutorbe.submission.domain.model.SubmissionStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeService {

    private final PistonClient pistonClient;
    private final ProblemService problemService;

    public JudgeResult processSubmission(SubmissionCreatedMessage message) {
        log.info("Bắt đầu chấm bài: submissionId={}", message.submissionId());
        List<TestcaseDto> testcases = problemService.getProblemTestcase(message.problemId());

        SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED;
        int passedCount = 0;

        for (int i = 0; i < testcases.size(); i++) {
            TestcaseDto testcase = testcases.get(i);
            log.debug("Chạy testcase {}/{}", i + 1, testcases.size());

            // Gọi Piston thực thi code
            PistonResponse response = pistonClient.executeCode(
                    message.language(),
                    message.sourceCode(),
                    testcase.input()
            );

            // Kiểm tra lỗi compile / runtime trước
            SubmissionStatus testcaseStatus = evaluateResponse(response);

            if (testcaseStatus != SubmissionStatus.ACCEPTED) {
                // Lỗi nghiêm trọng → dừng sớm, không cần chạy tiếp
                finalStatus = testcaseStatus;
                log.warn("submissionId={} thất bại tại testcase {}: {}",
                        message.submissionId(), i + 1, testcaseStatus);
                break;
            }

            // So sánh output
            String actualOutput = response.run().output();
            if (compareOutput(actualOutput, testcase.expectedOutput())) {
                passedCount++;
            } else {
                finalStatus = SubmissionStatus.WRONG_ANSWER;
                log.debug("Testcase {} sai. Expected=[{}], Actual=[{}]",
                        i + 1, testcase.expectedOutput(), actualOutput);
            }
        }

        log.info("Kết quả submissionId={}: status={}, passed={}/{}",
                message.submissionId(), finalStatus, passedCount, testcases.size());

        return new JudgeResult(finalStatus, passedCount, testcases.size());
    }

    /**
     * Đánh giá response từ Piston, trả về status tương ứng.
     * Tách riêng để dễ test và tái sử dụng.
     */
    private SubmissionStatus evaluateResponse(PistonResponse response) {
        // Compile error
        if (response.compile() != null && response.compile().code() != 0) {
            return SubmissionStatus.COMPILE_ERROR;
        }

        // Runtime error hoặc TLE
        if (response.run().code() != 0) {
            if ("SIGKILL".equals(response.run().signal())) {
                return SubmissionStatus.TIME_LIMIT_EXCEEDED;
            }
            return SubmissionStatus.RUNTIME_ERROR;
        }

        return SubmissionStatus.ACCEPTED;
    }

    /**
     * Chuẩn hóa và so sánh output an toàn
     */
    private boolean compareOutput(String actual, String expected) {
        if (actual == null || expected == null) return false;
        String normalizedActual = actual.trim().replace("\r\n", "\n");
        String normalizedExpected = expected.trim().replace("\r\n", "\n");
        return normalizedActual.equals(normalizedExpected);
    }
}
