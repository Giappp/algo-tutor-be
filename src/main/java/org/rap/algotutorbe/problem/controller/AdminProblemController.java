package org.rap.algotutorbe.problem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.request.AIContextRequest;
import org.rap.algotutorbe.problem.application.dto.request.ModelSolutionRequest;
import org.rap.algotutorbe.problem.application.dto.request.RunTestcasesRequest;
import org.rap.algotutorbe.problem.application.dto.request.UpdateProblemRequest;
import org.rap.algotutorbe.problem.application.dto.response.AIContextResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemDetailAdminResponse;
import org.rap.algotutorbe.problem.application.dto.response.ProblemSummaryAdminResponse;
import org.rap.algotutorbe.problem.application.services.AdminProblemService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/problems")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProblemController {
    private final AdminProblemService adminService;


    @GetMapping
    public ResponseEntity<PageResponse<ProblemSummaryAdminResponse>> listProblems(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.listProblems(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProblemDetailAdminResponse>> getProblem(@PathVariable Long id) {
        var result = adminService.getProblemDetail(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProblemSummaryAdminResponse>> createProblem(@RequestBody @Valid CreateProblemDto dto) {
        log.info("Creating problem {}", dto);
        var result = adminService.createProblem(dto);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProblemDetailAdminResponse>> updateProblem(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProblemRequest request
    ) {
        var result = adminService.updateProblem(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<String>> archiveProblem(@PathVariable Long id) {
        adminService.archiveProblem(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess("Problem archived"));
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<ApiResponse<String>> unarchiveProblem(@PathVariable Long id) {
        adminService.unarchiveProblem(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess("Problem unarchived (status set to DRAFT)"));
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
    public ResponseEntity<ApiResponse<ProblemDetailAdminResponse>> updateSolution(
            @PathVariable Long id,
            @Valid @RequestBody ModelSolutionRequest request
    ) {
        var result = adminService.updateModelSolution(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ProblemDetailAdminResponse>> publishProblem(@PathVariable Long id) {
        var result = adminService.publishProblem(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }


    @GetMapping("/{id}/ai-context")
    public ResponseEntity<ApiResponse<AIContextResponse>> getProblemAIContext(@PathVariable Long id) {
        var result = adminService.getAIContext(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PutMapping("/{id}/ai-context")
    public ResponseEntity<ApiResponse<AIContextResponse>> updateAIContext(
            @PathVariable Long id,
            @RequestBody @Valid AIContextRequest request
    ) {
        var result = adminService.updateAIContext(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }
}

