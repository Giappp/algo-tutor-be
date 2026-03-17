package org.rap.algotutorbe.problem.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.request.AIContextRequest;
import org.rap.algotutorbe.problem.application.dto.request.ModelSolutionRequest;
import org.rap.algotutorbe.problem.application.dto.request.UpsertTestcasesRequest;
import org.rap.algotutorbe.problem.application.services.AdminProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/problems")
@Slf4j
@RequiredArgsConstructor
public class AdminProblemController {
    private final AdminProblemService adminService;

    @PostMapping("/")
    public ResponseEntity<?> createProblem(@RequestBody @Valid CreateProblemDto dto) {
        log.info("Creating problem {}", dto);
        Long authorId = 1L; // TODO: Get from auth context
        var result = adminService.createProblem(dto, authorId);
        return ResponseEntity.ok(ApiResponse.builder()
                .data(result)
                .success(true)
                .messages("Problem created")
                .build());
    }

    @PostMapping("/{id}/test-cases")
    public ResponseEntity<?> upsertTestcases(
            @PathVariable Long id,
            @Valid @RequestBody UpsertTestcasesRequest req
    ) {
        return ResponseEntity.ok(adminService.upsertTestcases(id, req));
    }

    @PostMapping("/{id}/model-solution")
    public ResponseEntity<?> updateSolution(@PathVariable Long id,
                                            @Valid @RequestBody ModelSolutionRequest request) {
        return ResponseEntity.ok(adminService.updateModelSolution(id, request));

    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<?> publishProblem(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.publishProblem(id));
    }

    @GetMapping("/{id}/ai-context")
    public ResponseEntity<?> getProblemAIContext(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAIContext(id));
    }

    @PutMapping("/{id}/ai-context")
    public ResponseEntity<?> updateAIContext(@PathVariable Long id, AIContextRequest request) {
        return ResponseEntity.ok(adminService.updateAIContext(id, request));
    }
}
