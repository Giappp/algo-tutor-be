package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.EnrollmentResponseDTO;
import org.rap.algotutorbe.learning.models.Enrollment;

@Mapper(config = GlobalMapperConfig.class)
public interface EnrollMapper {
    EnrollmentResponseDTO toResponse(Enrollment enrollment);
}
