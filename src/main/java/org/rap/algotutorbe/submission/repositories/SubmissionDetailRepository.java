package org.rap.algotutorbe.submission.repositories;

import org.rap.algotutorbe.submission.entities.SubmissionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionDetailRepository extends JpaRepository<SubmissionDetail, Long> {
}
