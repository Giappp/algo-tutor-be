package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.rap.algotutorbe.learning.mapper.RoadmapMapper;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LandingService {
    private final LearningPathRepository learningPathRepository;
    private final RoadmapMapper roadmapMapper;

    @Transactional(readOnly = true)
    public List<RoadmapResponseDTO> getPublishedRoadmaps() {
        List<LearningPath> roadmaps = learningPathRepository.findByDeletedFalseAndIsPublishedTrue();
        return roadmaps.stream()
                .map(roadmapMapper::toResponse)
                .toList();
    }
}
