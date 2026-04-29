package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.TopicRequestDTO;
import org.rap.algotutorbe.learning.models.Topic;

@Mapper(config = GlobalMapperConfig.class)
public interface TopicMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    @Mapping(target = "learningPath", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    Topic toEntity(TopicRequestDTO request);
}
