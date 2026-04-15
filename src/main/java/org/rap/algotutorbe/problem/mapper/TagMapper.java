package org.rap.algotutorbe.problem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.problem.application.dto.TagDto;
import org.rap.algotutorbe.problem.domain.models.Tag;

@Mapper(componentModel = "spring")
public interface TagMapper {
    Tag toEntity(TagDto dto);

    @Mapping(target = "numberOfProblems", ignore = true)
    TagDto toDto(Tag tag);
}
