package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.EnrollmentDetailResponseDTO;
import org.rap.algotutorbe.learning.models.Enrollment;

@Mapper(config = GlobalMapperConfig.class, uses = {LessonProgressionMapper.class})
public interface EnrollmentDetailMapper {

    @Mapping(target = "userId", expression = "java(entity.getUser().getId())")
    @Mapping(target = "learningPathId", expression = "java(entity.getLearningPath().getId())")
    @Mapping(target = "learningPathName", expression = "java(entity.getLearningPath().getName())")
    EnrollmentDetailResponseDTO toDto(Enrollment entity);
}
