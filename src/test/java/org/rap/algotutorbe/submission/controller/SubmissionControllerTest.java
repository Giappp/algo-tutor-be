package org.rap.algotutorbe.submission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.dto.SubmitCodeRequest;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
class SubmissionControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SubmissionService submissionService;

    @Test
    void submit_returnsWrappedSubmission() throws Exception {
        when(submissionService.submitCode(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SubmissionResponse(
                        "00000000-0000-0000-0000-000000000000",
                        "two-sum",
                        "python",
                        "pending",
                        0,
                        2,
                        null,
                        null,
                        Instant.now()
                ));

        var req = new SubmitCodeRequest("two-sum", "python", "print(1)", null);

        mvc.perform(post("/api/v1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.problemSlug").value("two-sum"))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }
}

