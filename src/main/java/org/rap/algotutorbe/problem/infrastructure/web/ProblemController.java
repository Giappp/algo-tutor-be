package org.rap.algotutorbe.problem.infrastructure.web;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.rap.algotutorbe.problem.application.services.ProblemService;
import org.rap.algotutorbe.problem.domain.enums.ProgrammingLanguage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/problems")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ProblemController {
    private final ProblemService problemService;

    @GetMapping
    public ResponseEntity<?> listProblems(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(problemService.listPublished(pageable));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getProblem(
            @PathVariable String slug,
            @RequestParam(defaultValue = "CPP") ProgrammingLanguage language
    ) {
        return ResponseEntity.ok(problemService.getPublishedBySlug(slug, language));
    }
}
