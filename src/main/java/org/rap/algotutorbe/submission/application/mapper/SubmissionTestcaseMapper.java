package org.rap.algotutorbe.submission.application.mapper;

import org.rap.algotutorbe.judge.application.dto.TestcaseResultDto;
import org.rap.algotutorbe.submission.domain.model.Submission;
import org.rap.algotutorbe.submission.domain.model.SubmissionTestcase;
import org.rap.algotutorbe.submission.domain.model.Verdict;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubmissionTestcaseMapper {

    public SubmissionTestcase toEntity(TestcaseResultDto dto, Submission submission) {
        SubmissionTestcase entity = new SubmissionTestcase();
        entity.setSubmission(submission);
        entity.setTestcaseIndex(dto.index());
        entity.setVerdict(Verdict.valueOf(dto.status()));
        entity.setTime((double) dto.cpuTime());
        entity.setMemory((int) dto.memory());
        return entity;
    }

    public List<SubmissionTestcase> toEntities(List<TestcaseResultDto> dtos, Submission submission) {
        return dtos.stream()
                .map(dto -> toEntity(dto, submission))
                .toList();
    }
}