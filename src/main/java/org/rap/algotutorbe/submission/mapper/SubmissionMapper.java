package org.rap.algotutorbe.submission.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.submission.dto.SubmissionDetailResponse;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.dto.SubmissionTestcaseResultResponse;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.Verdict;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface SubmissionMapper {

    @Named("languageToApiValue")
    default String languageToApiValue(ProgrammingLanguage language) {
        return language != null ? language.toApiValue() : null;
    }

    @Named("verdictToApiValue")
    default String verdictToApiValue(Verdict verdict) {
        return verdict != null ? verdict.toApiValue() : null;
    }

    @Mapping(target = "language", source = "language", qualifiedByName = "languageToApiValue")
    @Mapping(target = "status", source = "verdict", qualifiedByName = "verdictToApiValue")
    @Mapping(target = "submittedAt", source = "createdAt")
    SubmissionResponse toResponse(Submission submission);

    @Mapping(target = "language", source = "submission.language", qualifiedByName = "languageToApiValue")
    @Mapping(target = "status", source = "submission.verdict", qualifiedByName = "verdictToApiValue")
    @Mapping(target = "passedTestCases", source = "submission.passedTestcases")
    @Mapping(target = "totalTestCases", source = "submission.totalTestcases")
    @Mapping(target = "submittedAt", source = "submission.createdAt")
    SubmissionDetailResponse toDetailResponse(Submission submission, List<SubmissionTestcaseResultResponse> results);
}
