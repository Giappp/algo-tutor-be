package org.rap.algotutorbe.judge0.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge0.application.dto.Judge0BatchSubmissionRequest;
import org.rap.algotutorbe.judge0.application.dto.Judge0TokenResponse;
import org.rap.algotutorbe.judge0.application.utils.Judge0Client;
import org.rap.algotutorbe.judge0.application.utils.Judge0PayloadBuilder;
import org.rap.algotutorbe.judge0.domain.Judge0SubmissionException;
import org.rap.algotutorbe.problem.application.dto.TestcaseDto;
import org.rap.algotutorbe.submission.domain.model.Submission;
import org.rap.algotutorbe.submission.domain.model.SubmissionTestcase;
import org.rap.algotutorbe.submission.domain.model.Verdict;
import org.rap.algotutorbe.submission.domain.repositories.SubmissionRepository;
import org.rap.algotutorbe.submission.domain.repositories.SubmissionTestcaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class Judge0Service {

    private final Judge0PayloadBuilder payloadBuilder;
    private final Judge0Client judge0Client;
    private final SubmissionTestcaseRepository testcaseRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional
    public void processAndSendToJudge0(Long submissionId, String sourceCode, int languageId, List<TestcaseDto> testcases) {
        log.info("Bắt đầu xử lý chấm bài submissionId={}, số lượng testcases={}", submissionId, testcases.size());

        // 1. Build Payload gửi đi
        Judge0BatchSubmissionRequest batchPayload = payloadBuilder.buildBatch(
                sourceCode, languageId, testcases, submissionId
        );
        Submission submission = getOrThrowSubmission(submissionId);
        // 2. Gọi HTTP qua Judge0 (Lưu ý: Không nên đánh @Transactional bao trùm quá trình gọi HTTP chậm,
        // nhưng ở ví dụ này gộp chung để dễ hình dung, nếu production bạn nên tách hàm gọi HTTP ra ngoài Transaction)
        List<Judge0TokenResponse> tokens = judge0Client.submitBatch(batchPayload);

        if (tokens == null || tokens.size() != testcases.size()) {
            throw new IllegalStateException("Số lượng token trả về từ Judge0 không khớp với số lượng testcase!");
        }

        // 3. Khởi tạo dữ liệu SubmissionTestcase trong Database với trạng thái PENDING
        List<SubmissionTestcase> testcaseEntities = new ArrayList<>();

        for (int i = 0; i < testcases.size(); i++) {
            TestcaseDto dto = testcases.get(i);
            String token = tokens.get(i).token();

            SubmissionTestcase entity = new SubmissionTestcase();
            entity.setSubmission(submission);
            entity.setTestcaseIndex(dto.orderIndex());
            entity.setJudge0Token(token);
            entity.setVerdict(Verdict.PROCESSING); // Trạng thái chờ Webhook gọi về

            testcaseEntities.add(entity);
        }

        // Lưu toàn bộ testcase vào DB (Dùng saveAll để tối ưu)
        testcaseRepository.saveAll(testcaseEntities);

        submission.setTotalTestcases(testcases.size());
        submissionRepository.save(submission);

        log.info("Đã gửi {} testcases sang Judge0 thành công cho submissionId={}", testcases.size(), submissionId);
    }

    private Submission getOrThrowSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new Judge0SubmissionException("Submission không tồn tại với id: " + submissionId));
    }
}
