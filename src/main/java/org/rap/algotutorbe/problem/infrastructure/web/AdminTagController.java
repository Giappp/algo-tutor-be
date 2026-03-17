package org.rap.algotutorbe.problem.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.application.dto.request.CreateTagRequest;
import org.rap.algotutorbe.problem.application.services.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/tags")
@Slf4j
@RequiredArgsConstructor
public class AdminTagController {
    private final TagService tagService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CreateTagRequest request) {
        var saved = tagService.create(request);
        return ResponseEntity.ok().body(ApiResponse
                .builder()
                .success(true)
                .data(saved)
                .build());
    }
}
