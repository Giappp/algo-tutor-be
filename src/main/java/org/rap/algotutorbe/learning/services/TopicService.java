package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.TopicRequestDTO;
import org.rap.algotutorbe.learning.dto.TopicResponseDTO;
import org.rap.algotutorbe.learning.mapper.TopicMapper;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final TopicRepository topicRepository;
    private final LearningPathRepository learningPathRepository;
    private final TopicMapper topicMapper;

    @Transactional
    public @Nullable ApiResponse<TopicResponseDTO> create(Long learningPathId, @Valid TopicRequestDTO request) {
        LearningPath learningPath = learningPathRepository.findById(learningPathId)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));

        Topic topic = topicMapper.toEntity(request);
        topic.setDisplayOrder(topicRepository.getNextDisplayOrder(learningPathId));
        topic.setLearningPath(learningPath);
        learningPath.getTopics().add(topic);

        learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(topicMapper.toResponse(topic));
    }

    @Transactional
    public @Nullable ApiResponse<TopicResponseDTO> update(Long topicId, @Valid TopicRequestDTO request) {
        Topic topic = getOrThrow(topicId);
        topicMapper.updateEntity(topic, request);
        Topic saved = topicRepository.save(topic);
        return ApiResponse.buildSuccess(topicMapper.toResponse(saved));
    }

    @Transactional
    public @Nullable ApiResponse<String> delete(Long topicId) {
        Topic topic = getOrThrow(topicId);
        topicRepository.delete(topic);
        return ApiResponse.buildMessage("Topic deleted successfully");
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<TopicResponseDTO> getById(Long topicId) {
        Topic topic = getOrThrow(topicId);
        return ApiResponse.buildSuccess(topicMapper.toResponse(topic));
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<List<TopicResponseDTO>> getByLearningPathId(Long learningPathId) {
        if (!learningPathRepository.existsById(learningPathId)) {
            throw new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND);
        }
        List<Topic> topics = topicRepository.findByLearningPathIdOrderByDisplayOrder(learningPathId);
        List<TopicResponseDTO> responses = topics.stream()
                .map(topicMapper::toResponse)
                .toList();
        return ApiResponse.buildSuccess(responses);
    }

    public Topic getOrThrow(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }
}
