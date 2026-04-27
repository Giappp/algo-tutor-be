package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.TopicRequestDTO;
import org.rap.algotutorbe.learning.models.Topic;

@Mapper(config = GlobalMapperConfig.class)
public interface TopicMapper {
    Topic toEntity(TopicRequestDTO request);
}
