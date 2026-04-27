package org.rap.algotutorbe.problem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.problem.dto.request.*;
import org.rap.algotutorbe.problem.dto.response.EditorialResponse;
import org.rap.algotutorbe.problem.dto.response.ProblemDetailAdminResponse;
import org.rap.algotutorbe.problem.dto.response.ProblemSummaryAdminResponse;
import org.rap.algotutorbe.problem.dto.response.testcase.TestcasesResponse;
import org.rap.algotutorbe.problem.services.AdminProblemService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/problems")
@Slf4j
@RequiredArgsConstructor
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
    public ResponseEntity<ApiResponse<ProblemSummaryAdminResponse>> createProblem(
            @RequestBody @Valid CreateProblemAdminRequest request
    ) {
        log.info("Creating problem {}", request);
        var result = adminService.createProblem(request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProblemDetailAdminResponse>> updateProblem(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProblemAdminRequest request
    ) {
        var result = adminService.updateProblem(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProblem(@PathVariable Long id) {
        adminService.archiveProblem(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess("Problem archived successfully"));
    }

    // ====================================================================================
    // STATUS MANAGEMENT
    // ====================================================================================

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

    // ====================================================================================
    // TESTCASES
    // ====================================================================================

    @PostMapping("/{id}/testcases")
    public ResponseEntity<ApiResponse<TestcasesResponse>> insertTestcases(
            @PathVariable Long id,
            @Valid @RequestBody TestcasesRequest request
    ) {
        var result = adminService.insertTestcases(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    // ====================================================================================
    // EDITORIALS
    // ====================================================================================

    @GetMapping("/{id}/editorials")
    public ResponseEntity<ApiResponse<List<EditorialResponse>>> getEditorials(@PathVariable Long id) {
        var result = adminService.getEditorials(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PostMapping("/{id}/editorials")
    public ResponseEntity<ApiResponse<EditorialResponse>> createEditorial(
            @PathVariable Long id,
            @RequestBody @Valid CreateEditorialRequest request
    ) {
        var result = adminService.createEditorial(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @PutMapping("/{id}/editorials/{editorialId}")
    public ResponseEntity<ApiResponse<EditorialResponse>> updateEditorial(
            @PathVariable Long id,
            @PathVariable Long editorialId,
            @RequestBody @Valid UpdateEditorialRequest request
    ) {
        var result = adminService.updateEditorial(id, editorialId, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }

    @DeleteMapping("/{id}/editorials/{editorialId}")
    public ResponseEntity<ApiResponse<String>> deleteEditorial(
            @PathVariable Long id,
            @PathVariable Long editorialId
    ) {
        adminService.deleteEditorial(id, editorialId);
        return ResponseEntity.ok(ApiResponse.buildSuccess("Editorial deleted"));
    }

    // ====================================================================================
    // MODEL SOLUTION (with validation)
    // ====================================================================================

    @PostMapping("/{id}/model-solution")
    public ResponseEntity<ApiResponse<ProblemDetailAdminResponse>> updateSolution(
            @PathVariable Long id,
            @Valid @RequestBody ModelSolutionRequest request
    ) {
        var result = adminService.updateModelSolution(id, request);
        return ResponseEntity.ok(ApiResponse.buildSuccess(result));
    }
}

