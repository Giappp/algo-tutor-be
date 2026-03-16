package org.rap.algotutorbe.problem.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.request.UpsertTestcasesRequest;
import org.rap.algotutorbe.problem.application.services.AdminProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/problems")
@RequiredArgsConstructor
public class AdminProblemController {
    private final AdminProblemService adminService;

    @PostMapping
    public ResponseEntity<?> createProblem(@RequestBody @Valid CreateProblemDto dto) {
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
}
