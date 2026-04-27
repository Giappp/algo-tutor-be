package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.LearningPathResponse;
import org.rap.algotutorbe.learning.dto.LearningPathSummaryResponse;
import org.rap.algotutorbe.learning.dto.request.CreateLearningPathRequest;
import org.rap.algotutorbe.learning.dto.request.UpdateLearningPathRequest;
import org.rap.algotutorbe.learning.models.LearningPath;

@Mapper(config = GlobalMapperConfig.class)
public interface LearningPathMapper {

    default LearningPathSummaryResponse toSummary(LearningPath learningPath) {
        long totalTopics = learningPath.getTopics() != null ? learningPath.getTopics().size() : 0;
        long totalLessons = learningPath.getTopics() != null
                ? learningPath.getTopics().stream().mapToLong(t -> t.getLessons().size()).sum()
                : 0;
        return new LearningPathSummaryResponse(
                learningPath.getId(),
                learningPath.getName(),
                learningPath.getSlug(),
                learningPath.getLevel(),
                learningPath.getDescription(),
                totalTopics,
                totalLessons,
                learningPath.getCreatedAt(),
                learningPath.getUpdatedAt()
        );
    }

    @Mapping(target = "topics", ignore = true)
    LearningPathResponse toDetail(LearningPath learningPath);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "slug", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "deleted", ignore = true)
    @org.mapstruct.Mapping(target = "topics", ignore = true)
    @org.mapstruct.Mapping(target = "enrollments", ignore = true)
    LearningPath toEntity(CreateLearningPathRequest request);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "slug", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "deleted", ignore = true)
    @org.mapstruct.Mapping(target = "topics", ignore = true)
    @org.mapstruct.Mapping(target = "enrollments", ignore = true)
    void updateEntity(UpdateLearningPathRequest request, @MappingTarget LearningPath learningPath);
}
