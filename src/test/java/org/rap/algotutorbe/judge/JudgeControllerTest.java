package org.rap.algotutorbe.judge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.rap.algotutorbe.submission.dto.SubmissionDetailResponse;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JudgeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JudgeService judgeService;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        JudgeController controller = new JudgeController(judgeService, rateLimiter, submissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getSubmission_shouldReturnLatestPersistedSubmissionDetail() throws Exception {
        UUID submissionId = UUID.randomUUID();
        SubmissionDetailResponse response = new SubmissionDetailResponse(
                submissionId.toString(),
                "JAVA",
                "PENDING",
                "public class Solution {}",
                2,
                10,
                30,
                1024,
                null,
                Instant.now()
        );

        when(submissionService.getSubmissionDetail(submissionId)).thenReturn(response);

        mockMvc.perform(get("/judge/submissions/" + submissionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(submissionId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.passedTestCases").value(2));

        verify(submissionService).getSubmissionDetail(submissionId);
    }
}
