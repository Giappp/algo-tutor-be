package org.rap.algotutorbe.problem.infrastructure.repositories;

import lombok.NonNull;
import org.rap.algotutorbe.problem.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<@NonNull Problem, @NonNull Long> {

    // Lấy problem cùng với các config ngôn ngữ để tránh N+1 khi hiển thị cho user
    @Query("SELECT p FROM Problem p LEFT JOIN FETCH p.languageConfigs WHERE p.id = :id")
    Optional<Problem> findByIdWithConfigs(@Param("id") Long id);

    // Lấy problem cùng toàn bộ test case (dùng cho worker khi chấm bài hoặc benchmark)
    @Query("SELECT p FROM Problem p LEFT JOIN FETCH p.testCases WHERE p.id = :id")
    Optional<Problem> findByIdWithTestCases(@Param("id") Long id);
    
}
