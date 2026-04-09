package org.rap.algotutorbe.problem.domain.repositories;

import org.rap.algotutorbe.problem.domain.models.Testcase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestcaseRepository extends JpaRepository<Testcase, Long> {
    @Query("SELECT t FROM Testcase t WHERE t.problem.id = :problemId ORDER BY t.orderIndex")
    Optional<List<Testcase>> findByProblemIdOrderByOrderIndex(Long problemId);

    @Query("SELECT t FROM Testcase t WHERE t.problem.id = :problemId AND t.isSample = true ORDER BY t.orderIndex")
    List<Testcase> findSamplesByProblemId(@Param("problemId") Long problemId);

    @Modifying
    @Query("DELETE FROM Testcase t WHERE t.problem.id = :problemId")
    void deleteAllByProblemId(@Param("problemId") Long problemId);

    List<Testcase> findByProblemId(Long problemId);
}
