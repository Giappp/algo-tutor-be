package org.rap.algotutorbe.submission.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.submission.dto.SubmissionDetailResponse;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.service.SubmissionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        SubmissionController controller = new SubmissionController(submissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getSubmission_shouldRouteToGetSubmissionDetail_whenIdIsUuid() throws Exception {
        UUID submissionId = UUID.randomUUID();
        SubmissionDetailResponse mockResponse = new SubmissionDetailResponse(
                submissionId.toString(),
                "JAVA",
                "ACCEPTED",
                "public class Solution {}",
                10,
                10,
                120,
                2048,
                "OK",
                true,
                Instant.now()
        );

        when(submissionService.getSubmissionDetail(submissionId)).thenReturn(mockResponse);

        mockMvc.perform(get("/submissions/" + submissionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(submissionId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.progressUpdated").value(true));
    }

    @Test
    void getSubmissionsByLessonSlug_shouldRouteToGetSubmissionsByLessonSlug_whenIdIsNotUuid() throws Exception {
        String lessonSlug = "two-sum";
        SubmissionResponse mockResponse = new SubmissionResponse(
                UUID.randomUUID().toString(),
                "JAVA",
                "ACCEPTED",
                10,
                10,
                120,
                2048,
                true,
                Instant.now()
        );

        when(submissionService.getSubmissionsByLessonSlug(lessonSlug)).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/submissions/" + lessonSlug)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].language").value("JAVA"))
                .andExpect(jsonPath("$.data[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data[0].progressUpdated").value(true));
    }
}
