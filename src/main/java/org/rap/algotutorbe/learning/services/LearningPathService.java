package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.common.services.SlugGenerator;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.dto.LearningPathResponseDTO;
import org.rap.algotutorbe.learning.dto.TopicRequestDTO;
import org.rap.algotutorbe.learning.enums.Level;
import org.rap.algotutorbe.learning.mapper.LearningPathMapper;
import org.rap.algotutorbe.learning.mapper.TopicMapper;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningPathService extends BaseService {
    private final LearningPathRepository learningPathRepository;
    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final LearningPathMapper learningPathMapper;
    private final TopicMapper topicMapper;
    private final SlugGenerator slugGenerator;

    @Transactional
    public @Nullable ApiResponse<Object> create(@Valid LearningPathRequestDTO request) {
        LearningPath learningPath = learningPathMapper.toEntity(request);
        String slug = generateUniqueSlug(request.name());
        learningPath.setSlug(slug);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }

    @Transactional
    public @Nullable ApiResponse<Object> update(Long id, @Valid LearningPathRequestDTO request) {
        LearningPath learningPath = getOrThrow(id);
        learningPathMapper.updateEntity(learningPath, request);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }

    @Transactional
    public @Nullable ApiResponse<Object> addTopic(Long id, @Valid TopicRequestDTO request) {
        LearningPath learningPath = getOrThrow(id);
        Topic topic = topicMapper.toEntity(request);
        learningPath.addTopic(topic);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getAll(Pageable pageable, Level level, String search) {
        Page<LearningPath> page;
        if (level != null && search != null && !search.isBlank()) {
            page = learningPathRepository.findByDeletedFalseAndLevelAndSearch(level, search.trim(), pageable);
        } else if (level != null) {
            page = learningPathRepository.findByDeletedFalseAndLevel(level, pageable);
        } else if (search != null && !search.isBlank()) {
            page = learningPathRepository.findByDeletedFalseAndSearch(search.trim(), pageable);
        } else {
            page = learningPathRepository.findByDeletedFalse(pageable);
        }

        List<LearningPathResponseDTO> content = page.getContent().stream()
                .map(learningPathMapper::toResponse)
                .toList();

        var meta = new java.util.HashMap<String, Object>();
        meta.put("page", page.getNumber());
        meta.put("size", page.getSize());
        meta.put("totalElements", page.getTotalElements());
        meta.put("totalPages", page.getTotalPages());
        meta.put("hasNext", page.hasNext());
        meta.put("hasPrevious", page.hasPrevious());

        return ApiResponse.buildSuccess(content, meta);
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<LearningPathResponseDTO> getBySlug(String slug) {
        LearningPath learningPath = learningPathRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
        return ApiResponse.buildSuccess(learningPathMapper.toResponse(learningPath));
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getById(Long id) {
        LearningPath learningPath = learningPathRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
        return ApiResponse.buildSuccess(learningPathMapper.toResponse(learningPath));
    }

    @Transactional
    public @Nullable ApiResponse<Object> delete(Long id) {
        LearningPath learningPath = getOrThrow(id);
        learningPath.setDeleted(true);
        learningPathRepository.save(learningPath);
        return ApiResponse.buildMessage("Learning path deleted successfully");
    }

    @Transactional
    public @Nullable ApiResponse<Object> togglePublish(Long id) {
        LearningPath learningPath = getOrThrow(id);
        learningPathRepository.save(learningPath);
        return ApiResponse.buildMessage("Learning path published status toggled");
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = slugGenerator.generateFrom(title);
        String candidate = baseSlug;
        int counter = 1;
        while (learningPathRepository.existsBySlug(candidate)) {
            candidate = baseSlug + "-" + counter++;
        }
        return candidate;
    }

    private LearningPath getOrThrow(Long id) {
        return learningPathRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
    }
}
