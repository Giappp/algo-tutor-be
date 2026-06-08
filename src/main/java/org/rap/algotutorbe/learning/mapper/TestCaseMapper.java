package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.testcase.SaveTestCaseRequest;
import org.rap.algotutorbe.learning.dto.testcase.TestCaseDTO;
import org.rap.algotutorbe.learning.models.Testcase;

@Mapper(config = GlobalMapperConfig.class)
public interface TestCaseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codingLesson", ignore = true)
    Testcase toEntity(SaveTestCaseRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codingLesson", ignore = true)
    void updateEntity(@MappingTarget Testcase entity, TestCaseDTO dto);

    TestCaseDTO toDto(Testcase entity);
}
