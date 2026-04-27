package org.rap.algotutorbe.submission.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.judge.dto.TestcaseJudgeResult;
import org.rap.algotutorbe.submission.dto.SubmissionTestcaseResultResponse;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.SubmissionTestcase;

import java.util.List;

@Mapper(componentModel = "spring")
public class SubmissionTestcaseMapper {

    public SubmissionTestcase toEntity(TestcaseJudgeResult result, Submission submission) {
        SubmissionTestcase entity = new SubmissionTestcase();
        entity.setSubmission(submission);
        entity.setTestcaseIndex(result.index());
        entity.setVerdict(result.verdict());
        entity.setTime(result.cpuTime() != null ? result.cpuTime().doubleValue() : null);
        entity.setMemory(result.memory() != null ? result.memory().intValue() : null);
        entity.setStdout(result.stdout());
        entity.setCompileOutput(result.compileOutput());
        return entity;
    }

    public List<SubmissionTestcase> toEntities(List<TestcaseJudgeResult> results, Submission submission) {
        return results.stream()
                .map(r -> toEntity(r, submission))
                .toList();
    }

    public SubmissionTestcaseResultResponse toResponse(SubmissionTestcase entity) {
        return new SubmissionTestcaseResultResponse(
                entity.getTestcaseIndex(),
                entity.getVerdict() != null ? entity.getVerdict().toApiValue() : null,
                null,
                null,
                entity.getStdout(),
                entity.getTime() != null ? entity.getTime().intValue() : null,
                entity.getCompileOutput()
        );
    }

    public List<SubmissionTestcaseResultResponse> toResponses(List<SubmissionTestcase> entities) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }
}