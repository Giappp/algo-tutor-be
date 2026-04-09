package org.rap.algotutorbe.problem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.request.AIContextRequest;
import org.rap.algotutorbe.problem.application.dto.request.ModelSolutionRequest;
import org.rap.algotutorbe.problem.application.dto.request.RunTestcasesRequest;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryAdminResponse;
import org.rap.algotutorbe.problem.application.services.AdminProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/problems")
@Slf4j
@RequiredArgsConstructor
public class AdminProblemController {
    private final AdminProblemService adminService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProblemSummaryAdminResponse>> createProblem(@RequestBody @Valid CreateProblemDto dto) {
        log.info("Creating problem {}", dto);
        var result = adminService.createProblem(dto);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PostMapping("/{id}/testcases")
    public ResponseEntity<ApiResponse<String>> upsertTestcases(
            @PathVariable Long id,
            @Valid @RequestBody RunTestcasesRequest req
    ) {
        adminService.upsertTestcases(id, req);
        return ResponseEntity.ok(ApiResponse.buildSuccess("All test cases passed"));
    }

    @PostMapping("/{id}/model-solution")
    public ResponseEntity<ApiResponse<Object>> updateSolution(@PathVariable Long id,
                                                              @Valid @RequestBody ModelSolutionRequest request) {
        var result = adminService.updateModelSolution(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));

    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Object>> publishProblem(@PathVariable Long id) {
        var result = adminService.publishProblem(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @GetMapping("/{id}/ai-context")
    public ResponseEntity<ApiResponse<Object>> getProblemAIContext(@PathVariable Long id) {
        var result = adminService.getAIContext(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PutMapping("/{id}/ai-context")
    public ResponseEntity<ApiResponse<Object>> updateAIContext(@PathVariable Long id, AIContextRequest request) {
        var result = adminService.updateAIContext(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }
}
