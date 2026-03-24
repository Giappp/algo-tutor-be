package org.rap.algotutorbe.problem.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.request.AIContextRequest;
import org.rap.algotutorbe.problem.application.dto.request.ModelSolutionRequest;
import org.rap.algotutorbe.problem.application.dto.request.UpsertTestcasesRequest;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryAdminResponse;
import org.rap.algotutorbe.problem.application.dto.response.TestcaseAdminResponse;
import org.rap.algotutorbe.problem.application.services.AdminProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/problems")
@Slf4j
@RequiredArgsConstructor
public class AdminProblemController {
    private final AdminProblemService adminService;

    @PostMapping("/")
    public ResponseEntity<ApiResponse<ProblemSummaryAdminResponse>> createProblem(@RequestBody @Valid CreateProblemDto dto) {
        log.info("Creating problem {}", dto);
        Long authorId = 1L; // TODO: Get from auth context
        var result = adminService.createProblem(dto, authorId);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PostMapping("/{id}/test-cases")
    public ResponseEntity<ApiResponse<List<TestcaseAdminResponse>>> upsertTestcases(
            @PathVariable Long id,
            @Valid @RequestBody UpsertTestcasesRequest req
    ) {
        var result = adminService.upsertTestcases(id, req);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PostMapping("/{id}/model-solution")
    public ResponseEntity<?> updateSolution(@PathVariable Long id,
                                            @Valid @RequestBody ModelSolutionRequest request) {
        var result = adminService.updateModelSolution(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));

    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<?> publishProblem(@PathVariable Long id) {
        var result = adminService.publishProblem(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @GetMapping("/{id}/ai-context")
    public ResponseEntity<?> getProblemAIContext(@PathVariable Long id) {
        var result = adminService.getAIContext(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PutMapping("/{id}/ai-context")
    public ResponseEntity<?> updateAIContext(@PathVariable Long id, AIContextRequest request) {
        var result = adminService.updateAIContext(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }
}
