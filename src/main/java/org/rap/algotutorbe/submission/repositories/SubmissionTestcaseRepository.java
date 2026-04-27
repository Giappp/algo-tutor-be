package org.rap.algotutorbe.submission.repositories;

import org.rap.algotutorbe.submission.entities.SubmissionTestcase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionTestcaseRepository extends JpaRepository<SubmissionTestcase, Long> {
    List<SubmissionTestcase> findBySubmissionId(UUID submissionId);
}
