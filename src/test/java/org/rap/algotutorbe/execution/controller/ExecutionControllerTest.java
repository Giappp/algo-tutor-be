package org.rap.algotutorbe.execution.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.execution.dto.ExecuteRequest;
import org.rap.algotutorbe.execution.dto.ExecuteResponse;
import org.rap.algotutorbe.execution.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExecutionController.class)
class ExecutionControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ExecutionService executionService;

    @Test
    void execute_returnsWrappedData() throws Exception {
        when(executionService.execute(any()))
                .thenReturn(new ExecuteResponse("ok", "", 0, 1, 2));

        var body = new ExecuteRequest("python", "print('ok')", null, 5000);

        mvc.perform(post("/api/v1/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stdout").value("ok"))
                .andExpect(jsonPath("$.data.exitCode").value(0));
    }
}
