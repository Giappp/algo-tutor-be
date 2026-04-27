package org.rap.algotutorbe.problem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.dto.TagDto;
import org.rap.algotutorbe.problem.dto.request.UpdateOrCreateTagRequest;
import org.rap.algotutorbe.problem.services.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/tags")
@Slf4j
@RequiredArgsConstructor
public class AdminTagController {
    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TagDto>>> getTags(@RequestParam(required = false) String keyword) {
        var tags = tagService.getTags(keyword);
        return ResponseEntity.ok(ApiResponse.buildSuccess(tags));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(@RequestBody @Valid UpdateOrCreateTagRequest request) {
        var saved = tagService.create(request);
        return ResponseEntity.ok().body(ApiResponse
                .builder()
                .success(true)
                .data(saved)
                .build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TagDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrCreateTagRequest request) {
        var updated = tagService.update(id, request);
        return ResponseEntity.ok().body(ApiResponse.buildSuccess(updated));
    }
}
