package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.common.services.SlugGenerator;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.dto.LearningPathResponseDTO;
import org.rap.algotutorbe.learning.enums.Level;
import org.rap.algotutorbe.learning.mapper.LearningPathMapper;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningPathService extends BaseService {
    private final LearningPathRepository learningPathRepository;
    private final LearningPathMapper learningPathMapper;
    private final SlugGenerator slugGenerator;

    @Transactional
    public ApiResponse<LearningPathResponseDTO> create(@Valid LearningPathRequestDTO request) {
        LearningPath learningPath = learningPathMapper.toEntity(request);
        String slug = slugGenerator.generateUniqueForLearningPath(request.name());
        learningPath.setSlug(slug);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(learningPathMapper.toResponse(saved));
    }

    @Transactional
    public ApiResponse<LearningPathResponseDTO> update(Long id, @Valid LearningPathRequestDTO request) {
        LearningPath learningPath = getOrThrow(id);
        learningPathMapper.updateEntity(learningPath, request);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(learningPathMapper.toResponse(saved));
    }

    @Transactional(readOnly = true)
    public PageResponse<LearningPathResponseDTO> getAll(Pageable pageable, Level level, String search) {
        Page<LearningPath> page;
        if (level != null && search != null && !search.isBlank()) {
            page = learningPathRepository.findByDeletedFalseAndLevelAndSearch(level, search.trim(), pageable);
        } else if (level != null) {
            page = learningPathRepository.findByDeletedFalseAndLevel(level, pageable);
        } else if (search != null && !search.isBlank()) {
            page = learningPathRepository.findByDeletedFalseAndSearch(search.trim(), pageable);
        } else {
            page = learningPathRepository.findAll(pageable);
        }
        return PageResponse.of(page.map(learningPathMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ApiResponse<LearningPathResponseDTO> getBySlug(String slug) {
        LearningPath learningPath = learningPathRepository.findBySlugAndNotDeleted(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
        return ApiResponse.buildSuccess(learningPathMapper.toResponse(learningPath));
    }

    @Transactional(readOnly = true)
    public ApiResponse<LearningPathResponseDTO> getById(Long id) {
        LearningPath learningPath = learningPathRepository.findByIdWithTopicsAndLessons(id)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
        return ApiResponse.buildSuccess(learningPathMapper.toResponse(learningPath));
    }

    @Transactional
    public ApiResponse<String> delete(Long id) {
        LearningPath learningPath = getOrThrow(id);
        learningPathRepository.delete(learningPath);
        return ApiResponse.buildMessage("Learning path deleted successfully");
    }

    @Transactional
    public ApiResponse<LearningPathResponseDTO> togglePublish(Long id) {
        LearningPath learningPath = getOrThrow(id);
        learningPath.setIsPublished(!Boolean.TRUE.equals(learningPath.getIsPublished()));
        learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(learningPathMapper.toResponse(learningPath));
    }

    private LearningPath getOrThrow(Long id) {
        return learningPathRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
    }
}
