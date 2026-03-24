package org.rap.algotutorbe.judge0.application.utils;

import org.rap.algotutorbe.judge0.application.dto.Judge0BatchSubmissionRequest;
import org.rap.algotutorbe.judge0.application.dto.Judge0SubmissionRequest;
import org.rap.algotutorbe.problem.application.dto.TestcaseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
public class Judge0PayloadBuilder {

    private static final double DEFAULT_CPU_TIME_LIMIT = 2.0;
    private static final int DEFAULT_MEMORY_LIMIT = 256_000;

    @Value("${judge0.callback.base-url}")
    private String callbackBaseUrl;

    public Judge0BatchSubmissionRequest buildBatch(
            String sourceCode,
            int languageId,
            List<TestcaseDto> testcases,
            Long submissionId
    ) {
        String encodedSourceCode = encodeBase64(sourceCode);

        List<Judge0SubmissionRequest> requests = testcases.stream()
                .map(tc -> new Judge0SubmissionRequest(
                        encodedSourceCode,
                        languageId,
                        encodeBase64(tc.input()),
                        encodeBase64(tc.expectedOutput()),
                        buildCallbackUrl(submissionId, tc.orderIndex()),
                        DEFAULT_CPU_TIME_LIMIT,
                        DEFAULT_MEMORY_LIMIT
                ))
                .toList();

        return new Judge0BatchSubmissionRequest(requests);
    }

    private String buildCallbackUrl(Long submissionId, Integer testcaseIndex) {
        // Webhook URL sẽ đính kèm thông tin để khi Judge0 gọi lại, ta biết kết quả là của testcase nào
        return callbackBaseUrl + "/api/webhook/judge0?submission_id=" + submissionId + "&testcase_index=" + testcaseIndex;
    }

    private String encodeBase64(String data) {
        if (data == null || data.isBlank()) return null;
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
}