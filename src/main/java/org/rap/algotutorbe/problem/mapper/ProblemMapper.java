package org.rap.algotutorbe.problem.mapper;

import org.mapstruct.*;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.problem.domain.models.Editorial;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.domain.models.Testcase;
import org.rap.algotutorbe.problem.dto.TagDto;
import org.rap.algotutorbe.problem.dto.request.CreateProblemAdminRequest;
import org.rap.algotutorbe.problem.dto.request.UpdateProblemAdminRequest;
import org.rap.algotutorbe.problem.dto.response.*;
import org.rap.algotutorbe.problem.dto.response.testcase.TestcaseAdminResponse;
import org.rap.algotutorbe.problem.dto.response.testcase.TestcaseSampleResponse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(config = GlobalMapperConfig.class, uses = TagMapper.class)
public interface ProblemMapper {

    // ========================================================================
    // Admin responses
    // ========================================================================

    @Mapping(target = "difficulty", source = "difficulty", qualifiedByName = "difficultyToString")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagsToNames")
    ProblemSummaryAdminResponse toSummaryAdmin(Problem problem);

    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagsToDtoSet")
    @Mapping(target = "editorials", source = "editorials", qualifiedByName = "editorialsToDtoSet")
    ProblemDetailAdminResponse toDetailAdmin(Problem problem);

    @Mapping(target = "isSample", source = "sample")
    TestcaseAdminResponse toTestcaseAdmin(Testcase testcase);

    EditorialResponse toEditorialResponse(Editorial editorial);

    // ========================================================================
    // Public responses
    // ========================================================================

    @Mapping(target = "difficulty", source = "difficulty", qualifiedByName = "difficultyToApiValue")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagsToNamesList")
    @Mapping(target = "examplesCount", source = "examples", qualifiedByName = "jsonNodeListSize")
    @Mapping(target = "hintsCount", source = "hints", qualifiedByName = "jsonNodeListSize")
    @Mapping(target = "status", constant = "not-started")
    ProblemSummaryResponse toSummary(Problem problem);

    @Mapping(target = "difficulty", source = "problem.difficulty", qualifiedByName = "difficultyToApiValue")
    @Mapping(target = "tags", source = "problem.tags", qualifiedByName = "tagsToNamesList")
    @Mapping(target = "examplesCount", source = "problem.examples", qualifiedByName = "jsonNodeListSize")
    @Mapping(target = "hintsCount", source = "problem.hints", qualifiedByName = "jsonNodeListSize")
    ProblemSummaryResponse toSummaryWithStatus(Problem problem, String status);

    @Mapping(target = "difficulty", source = "difficulty", qualifiedByName = "difficultyToApiValue")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagsToNamesList")
    @Mapping(target = "description", source = "statement")
    @Mapping(target = "examples", source = "examples")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToString")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "instantToString")
    @Mapping(target = "relatedSlugs", ignore = true)
        // Will be set in service layer
    ProblemDetailResponse toDetail(Problem problem, @Context List<String> relatedSlugs, @Context String userStatus);

    TestcaseSampleResponse toTestcaseSample(Testcase tc);

    // ========================================================================
    // DTO to Entity
    // ========================================================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "editorials", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    Problem toEntity(CreateProblemAdminRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "editorials", ignore = true)
    @Mapping(target = "lesson", ignore = true) // Will be set in service layer
    @Mapping(target = "submissions", ignore = true)
    void updateEntity(UpdateProblemAdminRequest request, @MappingTarget Problem problem);

    // ========================================================================
    // Named mappings for complex transformations
    // ========================================================================

    @Named("difficultyToString")
    default String difficultyToString(org.rap.algotutorbe.problem.domain.enums.Difficulty difficulty) {
        return difficulty != null ? difficulty.name() : null;
    }

    @Named("difficultyToApiValue")
    default String difficultyToApiValue(org.rap.algotutorbe.problem.domain.enums.Difficulty difficulty) {
        return difficulty != null ? difficulty.toApiValue() : null;
    }

    @Named("tagsToNames")
    default Set<String> tagsToNames(Set<Tag> tags) {
        if (tags == null) return Set.of();
        return tags.stream().map(Tag::getName).collect(Collectors.toSet());
    }

    @Named("tagsToNamesList")
    default List<String> tagsToNamesList(Set<Tag> tags) {
        if (tags == null) return List.of();
        return tags.stream().map(Tag::getName).toList();
    }

    @Named("tagsToDtoSet")
    default Set<TagDto> tagsToDtoSet(Set<Tag> tags) {
        if (tags == null) return Set.of();
        return tags.stream().map(tag -> new TagDto(tag.getId(), tag.getName(), null)).collect(Collectors.toSet());
    }

    @Named("editorialsToDtoSet")
    default Set<EditorialResponse> editorialsToDtoSet(List<Editorial> editorials) {
        if (editorials == null) return Set.of();
        return editorials.stream().map(this::toEditorialResponse).collect(Collectors.toSet());
    }

    @Named("jsonNodeListSize")
    default int jsonNodeListSize(Object node) {
        if (node == null) return 0;
        if (node instanceof List<?> l) return l.size();
        if (node instanceof com.fasterxml.jackson.databind.JsonNode jsonNode && jsonNode.isArray()) {
            return jsonNode.size();
        }
        return 0;
    }

    @Named("instantToString")
    default String instantToString(java.time.Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
