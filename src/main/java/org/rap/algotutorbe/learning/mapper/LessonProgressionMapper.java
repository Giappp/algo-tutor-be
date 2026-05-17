package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.LessonProgressionDTO;
import org.rap.algotutorbe.learning.models.LessonProgress;

@Mapper(config = GlobalMapperConfig.class)
public interface LessonProgressionMapper {

    default LessonProgressionDTO toDto(LessonProgress entity) {
        if (entity == null) {
            return null;
        }
        return new LessonProgressionDTO(
                entity.getLesson().getId(),
                entity.getEffectiveStatus(),
                entity.getUpdatedAt()
        );
    }
}
