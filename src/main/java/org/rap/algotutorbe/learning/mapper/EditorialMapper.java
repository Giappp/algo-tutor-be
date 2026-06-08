package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.EditorialResponseDTO;
import org.rap.algotutorbe.learning.models.Editorial;

@Mapper(config = GlobalMapperConfig.class)
public interface EditorialMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codingLesson", ignore = true)
    Editorial toEntity(org.rap.algotutorbe.learning.dto.EditorialRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codingLesson", ignore = true)
    void updateEntity(@MappingTarget Editorial entity, org.rap.algotutorbe.learning.dto.EditorialRequestDTO dto);

    EditorialResponseDTO toResponse(Editorial entity);
}
