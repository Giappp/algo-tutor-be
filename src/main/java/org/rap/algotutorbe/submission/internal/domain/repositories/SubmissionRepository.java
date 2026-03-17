package org.rap.algotutorbe.submission.internal.domain.repositories;

import org.rap.algotutorbe.submission.internal.domain.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
}
