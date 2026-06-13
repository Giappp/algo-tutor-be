package org.rap.algotutorbe.submission.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.submission.dto.SubmissionDetailResponse;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.SubmissionDetail;
import org.rap.algotutorbe.submission.entities.Verdict;

import java.util.Comparator;
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

    @Named("detailsToTestCases")
    default List<SubmissionDetailResponse.TestCaseResult> detailsToTestCases(List<SubmissionDetail> details) {
        if (details == null) {
            return List.of();
        }
        return details.stream()
                .sorted(Comparator.comparing(
                        detail -> detail.getTestcase().getSortOrder(),
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .map(detail -> new SubmissionDetailResponse.TestCaseResult(
                        detail.getTestcase().getSortOrder(),
                        verdictToApiValue(detail.getVerdict()),
                        detail.getTime(),
                        detail.getMemory(),
                        detail.getStdout(),
                        detail.getStderr()
                ))
                .toList();
    }

    @Mapping(target = "language", source = "language", qualifiedByName = "languageToApiValue")
    @Mapping(target = "status", source = "verdict", qualifiedByName = "verdictToApiValue")
    @Mapping(target = "submittedAt", source = "createdAt")
    SubmissionResponse toResponse(Submission submission);

    @Mapping(target = "language", source = "submission.language", qualifiedByName = "languageToApiValue")
    @Mapping(target = "status", source = "submission.verdict", qualifiedByName = "verdictToApiValue")
    @Mapping(target = "sourceCode", source = "submission.sourceCode")
    @Mapping(target = "passedTestCases", source = "submission.passedTestcases")
    @Mapping(target = "totalTestCases", source = "submission.totalTestcases")
    @Mapping(target = "submittedAt", source = "submission.createdAt")
    @Mapping(target = "testCases", source = "submission.submissionDetails", qualifiedByName = "detailsToTestCases")
    SubmissionDetailResponse toDetailResponse(Submission submission);
}
