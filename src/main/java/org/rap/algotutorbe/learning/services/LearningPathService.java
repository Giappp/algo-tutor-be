package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.dto.TopicRequestDTO;
import org.rap.algotutorbe.learning.mapper.LearningPathMapper;
import org.rap.algotutorbe.learning.mapper.TopicMapper;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LearningPathService extends BaseService {
    private final LearningPathRepository learningPathRepository;
    private final LearningPathMapper learningPathMapper;
    private final TopicMapper topicMapper;

    public @Nullable ApiResponse<Object> create(@Valid LearningPathRequestDTO request) {
        LearningPath learningPath = learningPathMapper.toEntity(request);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }

    public @Nullable ApiResponse<Object> update(Long id, @Valid LearningPathRequestDTO request) {
        LearningPath learningPath = getOrThrow(id);
        learningPathMapper.updateEntity(learningPath, request);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }

    private LearningPath getOrThrow(Long id) {
        return learningPathRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
    }

    public @Nullable ApiResponse<Object> addTopic(Long id, @Valid TopicRequestDTO request) {
        LearningPath learningPath = getOrThrow(id);
        Topic topic = topicMapper.toEntity(request);
        learningPath.addTopic(topic);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }
}
