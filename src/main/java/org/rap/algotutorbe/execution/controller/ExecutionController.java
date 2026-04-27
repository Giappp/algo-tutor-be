package org.rap.algotutorbe.execution.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.execution.dto.ExecuteRequest;
import org.rap.algotutorbe.execution.dto.ExecuteResponse;
import org.rap.algotutorbe.execution.dto.ExecuteTestRequest;
import org.rap.algotutorbe.execution.dto.ExecuteTestResponse;
import org.rap.algotutorbe.execution.service.ExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/execution")
@RequiredArgsConstructor
public class ExecutionController {
    private final ExecutionService executionService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExecuteResponse>> execute(@Valid @RequestBody ExecuteRequest request) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(executionService.execute(request)));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<ExecuteTestResponse>> executeTest(@Valid @RequestBody ExecuteTestRequest request) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(executionService.executeWithTestcases(request)));
    }
}

