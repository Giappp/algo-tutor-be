package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.LessonProgressionDTO;
import org.rap.algotutorbe.learning.enums.ProgressStatus;
import org.rap.algotutorbe.learning.models.LessonProgress;

@Mapper(config = GlobalMapperConfig.class)
public interface LessonProgressionMapper {

    default LessonProgressionDTO toDto(LessonProgress entity) {
        if (entity == null) {
            return null;
        }
        ProgressStatus status = Boolean.TRUE.equals(entity.getIsCompleted())
                ? ProgressStatus.COMPLETED
                : ProgressStatus.NOT_STARTED;
        return new LessonProgressionDTO(
                entity.getLesson().getId(),
                status,
                entity.getUpdatedAt()
        );
    }
}
