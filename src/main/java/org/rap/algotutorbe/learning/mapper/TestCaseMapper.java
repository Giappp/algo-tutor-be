package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.TestCaseDTO;
import org.rap.algotutorbe.learning.dto.TestCaseResponseDTO;
import org.rap.algotutorbe.learning.models.Testcase;

@Mapper(config = GlobalMapperConfig.class)
public interface TestCaseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codingLesson", ignore = true)
    Testcase toEntity(TestCaseDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codingLesson", ignore = true)
    void updateEntity(@MappingTarget Testcase entity, TestCaseDTO dto);

    TestCaseResponseDTO toResponse(Testcase entity);
}
