package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.common.services.SlugGenerator;
import org.rap.algotutorbe.learning.dto.LearningPathResponse;
import org.rap.algotutorbe.learning.dto.LearningPathSummaryResponse;
import org.rap.algotutorbe.learning.dto.request.CreateLearningPathRequest;
import org.rap.algotutorbe.learning.dto.request.UpdateLearningPathRequest;
import org.rap.algotutorbe.learning.mapper.LearningPathMapper;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Level;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.rap.algotutorbe.learning.repositories.LearningPathSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningPathService extends BaseService {

    private final LearningPathRepository learningPathRepository;
    private final LearningPathMapper learningPathMapper;
    private final SlugGenerator slugGenerator;

    @Transactional(readOnly = true)
    public PageResponse<LearningPathSummaryResponse> list(Level level, String search, Pageable pageable) {
        Specification<LearningPath> spec = LearningPathSpecifications.isActive();
        if (level != null) spec = spec.and(LearningPathSpecifications.hasLevel(level));
        if (search != null && !search.isBlank()) {
            spec = spec.and(LearningPathSpecifications.searchByName(search));
        }
        Page<LearningPath> page = learningPathRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(learningPathMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public LearningPathResponse getBySlug(String slug) {
        LearningPath learningPath = learningPathRepository.findBySlug(slug)
                .filter(lp -> !lp.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
        return learningPathMapper.toDetail(learningPath);
    }

    public LearningPathSummaryResponse create(CreateLearningPathRequest request) {
        String slug = (request.slug() != null && !request.slug().isBlank())
                ? request.slug()
                : slugGenerator.generateFrom(request.name());
        if (learningPathRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.LEARNING_PATH_SLUG_ALREADY_EXISTS);
        }
        LearningPath entity = learningPathMapper.toEntity(request);
        entity.setSlug(slug);
        LearningPath saved = learningPathRepository.save(entity);
        return learningPathMapper.toSummary(saved);
    }

    public LearningPathSummaryResponse update(Long id, UpdateLearningPathRequest request) {
        LearningPath entity = learningPathRepository.findById(id)
                .filter(lp -> !lp.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));

        if (request.slug() != null
                && !request.slug().equals(entity.getSlug())
                && learningPathRepository.existsBySlug(request.slug())) {
            throw new AppException(ErrorCode.LEARNING_PATH_SLUG_ALREADY_EXISTS);
        }
        learningPathMapper.updateEntity(request, entity);
        LearningPath saved = learningPathRepository.save(entity);
        return learningPathMapper.toSummary(saved);
    }

    public void delete(Long id) {
        LearningPath entity = learningPathRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
        entity.setDeleted(true);
        learningPathRepository.save(entity);
    }
}
