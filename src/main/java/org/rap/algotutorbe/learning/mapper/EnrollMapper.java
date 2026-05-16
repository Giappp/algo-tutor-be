package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.EnrollmentResponseDTO;
import org.rap.algotutorbe.learning.models.Enrollment;

@Mapper(config = GlobalMapperConfig.class)
public interface EnrollMapper {
    @Mapping(target = "userId", expression = "java(enrollment.getUser().getId())")
    @Mapping(target = "learningPathId", expression = "java(enrollment.getLearningPath().getId())")
    @Mapping(target = "learningPathName", expression = "java(enrollment.getLearningPath().getName())")
    @Mapping(target = "enrolledAt", expression = "java(enrollment.getEnrolledAt())")
    EnrollmentResponseDTO toResponse(Enrollment enrollment);
}
