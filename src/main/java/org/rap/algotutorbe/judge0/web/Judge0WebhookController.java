package org.rap.algotutorbe.judge0.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge0.application.dto.Judge0WebhookPayload;
import org.rap.algotutorbe.judge0.application.services.Judge0WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook/judge0")
@RequiredArgsConstructor
public class Judge0WebhookController {

    private final Judge0WebhookService webhookService;

    @PutMapping
    public ResponseEntity<Void> handleCallback(
            @RequestParam("submission_id") Long submissionId,
            @RequestParam("testcase_index") Integer testcaseIndex,
            @RequestBody Judge0WebhookPayload payload
    ) {
        log.info("Nhận Webhook từ Judge0: submissionId={}, testcaseIndex={}, status={}",
                submissionId, testcaseIndex, payload.status().description());

        webhookService.processResult(submissionId, testcaseIndex, payload);

        // Luôn trả về 200 OK để Judge0 biết mình đã nhận thành công, tránh bị gọi lại (retry)
        return ResponseEntity.ok().build();
    }
}