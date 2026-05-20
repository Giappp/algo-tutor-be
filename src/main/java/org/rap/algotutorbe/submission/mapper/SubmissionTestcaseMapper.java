package org.rap.algotutorbe.submission.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.submission.dto.SubmissionTestcaseResultResponse;
import org.rap.algotutorbe.submission.entities.SubmissionTestcase;

import java.util.List;

@Mapper(componentModel = "spring")
public class SubmissionTestcaseMapper {

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
