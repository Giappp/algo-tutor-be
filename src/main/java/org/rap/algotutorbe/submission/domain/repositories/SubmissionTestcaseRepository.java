package org.rap.algotutorbe.submission.domain.repositories;

import org.rap.algotutorbe.submission.domain.model.SubmissionTestcase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionTestcaseRepository extends JpaRepository<SubmissionTestcase, Long> {
    Optional<SubmissionTestcase> findBySubmissionIdAndTestcaseIndex(Long submissionId, Integer testcaseIndex);

    List<SubmissionTestcase> findBySubmissionId(Long submissionId);

}
