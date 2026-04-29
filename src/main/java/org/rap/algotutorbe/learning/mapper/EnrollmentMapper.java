package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.EnrollmentResponseDTO;
import org.rap.algotutorbe.learning.models.Enrollment;

@Mapper(config = GlobalMapperConfig.class)
public interface EnrollmentMapper {

    @Mapping(target = "learningPathName", expression = "java(entity.getLearningPath() != null ? entity.getLearningPath().getName() : null)")
    @Mapping(target = "userId", expression = "java(entity.getUser() != null ? entity.getUser().getId() : null)")
    @Mapping(target = "learningPathId", expression = "java(entity.getLearningPath() != null ? entity.getLearningPath().getId() : null)")
    EnrollmentResponseDTO toResponse(Enrollment entity);
}
