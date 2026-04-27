package org.rap.algotutorbe.submission.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.submission.dto.SubmissionDetailResponse;
import org.rap.algotutorbe.submission.dto.SubmissionResponse;
import org.rap.algotutorbe.submission.dto.SubmissionTestcaseResultResponse;
import org.rap.algotutorbe.submission.entities.Submission;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {
    default SubmissionResponse toResponse(Submission submission) {
        if (submission == null) return null;
        return new SubmissionResponse(
                submission.getId() != null ? submission.getId().toString() : null,
                submission.getLanguage() != null ? submission.getLanguage().toApiValue() : null,
                submission.getVerdict() != null ? submission.getVerdict().toApiValue() : null,
                submission.getPassedTestcases(),
                submission.getTotalTestcases(),
                submission.getMaxTime(),
                submission.getMaxMemory(),
                submission.getCreatedAt()
        );
    }

    default SubmissionDetailResponse toDetailResponse(Submission submission, List<SubmissionTestcaseResultResponse> results) {
        if (submission == null) return null;
        return new SubmissionDetailResponse(
                submission.getId() != null ? submission.getId().toString() : null,
                submission.getLanguage() != null ? submission.getLanguage().toApiValue() : null,
                submission.getVerdict() != null ? submission.getVerdict().toApiValue() : null,
                submission.getPassedTestcases(),
                submission.getTotalTestcases(),
                submission.getMaxTime(),
                submission.getMaxMemory(),
                submission.getCompileOutput(),
                results,
                submission.getCreatedAt()
        );
    }
}
