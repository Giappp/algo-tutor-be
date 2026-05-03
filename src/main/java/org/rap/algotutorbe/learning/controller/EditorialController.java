package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.EditorialRequestDTO;
import org.rap.algotutorbe.learning.dto.EditorialResponseDTO;
import org.rap.algotutorbe.learning.services.EditorialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/editorials")
@RequiredArgsConstructor
public class EditorialController {
    private final EditorialService editorialService;

    @PostMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EditorialResponseDTO>> createEditorial(
            @PathVariable Long lessonId,
            @RequestBody @Valid EditorialRequestDTO request) {
        return ResponseEntity.ok(editorialService.create(lessonId, request));
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<List<EditorialResponseDTO>>> getEditorialsByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(editorialService.getByLessonId(lessonId));
    }

    @PutMapping("/{editorialId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EditorialResponseDTO>> updateEditorial(
            @PathVariable Long editorialId,
            @RequestBody @Valid EditorialRequestDTO request) {
        return ResponseEntity.ok(editorialService.update(editorialId, request));
    }

    @DeleteMapping("/{editorialId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteEditorial(@PathVariable Long editorialId) {
        return ResponseEntity.ok(editorialService.delete(editorialId));
    }
}
