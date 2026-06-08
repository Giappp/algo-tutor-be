package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.LessonWithProgressDTO;
import org.rap.algotutorbe.learning.models.Lesson;

@Mapper(config = GlobalMapperConfig.class,
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface LessonWithProgressMapper {

    LessonWithProgressDTO toDto(Lesson entity);
}
