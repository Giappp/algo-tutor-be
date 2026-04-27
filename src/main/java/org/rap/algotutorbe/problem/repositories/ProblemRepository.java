package org.rap.algotutorbe.problem.repositories;

import lombok.NonNull;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<@NonNull Problem, @NonNull Long>, JpaSpecificationExecutor<Problem> {

    @Query("SELECT p FROM Problem p LEFT JOIN FETCH p.testCases WHERE p.id = :id")
    Optional<Problem> findByIdWithTestCases(@Param("id") Long id);
    
    @Query("""
            SELECT p FROM Problem p
            LEFT JOIN FETCH p.tags
            """)
    Page<Problem> findAllForAdmin(Pageable pageable);

}
