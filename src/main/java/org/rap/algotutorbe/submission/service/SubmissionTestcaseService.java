package org.rap.algotutorbe.submission.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.dto.JudgeResult;
import org.rap.algotutorbe.submission.mapper.SubmissionTestcaseMapper;
import org.rap.algotutorbe.submission.entities.Submission;
import org.rap.algotutorbe.submission.entities.SubmissionTestcase;
import org.rap.algotutorbe.submission.repositories.SubmissionTestcaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionTestcaseService {
    private final SubmissionTestcaseRepository submissionTestcaseRepository;
    private final SubmissionTestcaseMapper mapper;

    @Transactional
    public void saveAll(JudgeResult judgeResult, Submission submission) {
        List<SubmissionTestcase> entities = mapper.toEntities(judgeResult.details(), submission);
        submissionTestcaseRepository.saveAll(entities);
        log.info("Saved {} testcase results for submissionId={}",
                entities.size(), submission.getId());
    }
}
