package org.rap.algotutorbe.problem.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.dto.TagDto;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagDto toDto(Tag tag);
}
