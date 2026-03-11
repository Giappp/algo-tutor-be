package org.rap.algotutorbe.problem.infrastructure.web;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.services.ProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/problems")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ProblemController {
    ProblemService problemService;

    @PostMapping
    public ResponseEntity<?> createProblem(CreateProblemDto dto) {
        var result = problemService.createProblem(dto);
        return ResponseEntity.ok(ApiResponse.builder()
                .data(result)
                .success(true)
                .message("Problem created")
                .build());
    }
}
