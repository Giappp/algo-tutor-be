package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.EditorialRequestDTO;
import org.rap.algotutorbe.learning.dto.EditorialResponseDTO;
import org.rap.algotutorbe.learning.mapper.EditorialMapper;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Editorial;
import org.rap.algotutorbe.learning.repositories.CodingLessonRepository;
import org.rap.algotutorbe.learning.repositories.EditorialRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EditorialService {
    private final EditorialRepository editorialRepository;
    private final LessonRepository lessonRepository;
    private final CodingLessonRepository codingLessonRepository;
    private final EditorialMapper editorialMapper;

    @Transactional
    public ApiResponse<EditorialResponseDTO> create(Long lessonId, @Valid EditorialRequestDTO dto) {
        CodingLesson lesson = getCodingLessonOrThrow(lessonId);

        Editorial editorial = editorialMapper.toEntity(dto);
        editorial.setCodingLesson(lesson);
        lesson.getEditorials().add(editorial);

        codingLessonRepository.save(lesson);
        return ApiResponse.buildSuccess(editorialMapper.toResponse(editorial));
    }

    @Transactional
    public ApiResponse<EditorialResponseDTO> update(Long editorialId, @Valid EditorialRequestDTO dto) {
        Editorial editorial = getOrThrow(editorialId);
        editorialMapper.updateEntity(editorial, dto);
        Editorial saved = editorialRepository.save(editorial);
        return ApiResponse.buildSuccess(editorialMapper.toResponse(saved));
    }

    @Transactional
    public ApiResponse<String> delete(Long editorialId) {
        editorialRepository.deleteById(editorialId);
        return ApiResponse.buildMessage("Editorial deleted successfully");
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<EditorialResponseDTO>> getByLessonId(Long lessonId) {
        getCodingLessonOrThrow(lessonId);
        List<Editorial> editorials = editorialRepository.findByCodingLessonId(lessonId);
        List<EditorialResponseDTO> responses = editorials.stream()
                .map(editorialMapper::toResponse)
                .toList();
        return ApiResponse.buildSuccess(responses);
    }

    private CodingLesson getCodingLessonOrThrow(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .filter(CodingLesson.class::isInstance)
                .map(l -> (CodingLesson) l)
                .orElseThrow(() -> new AppException(ErrorCode.CODING_LESSON_REQUIRED));
    }

    private Editorial getOrThrow(Long id) {
        return editorialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EDITORIAL_NOT_FOUND));
    }
}
