package org.rap.algotutorbe.problem.domain.repositories;

import org.rap.algotutorbe.problem.domain.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    @Query("""
                SELECT t FROM Tag t
                WHERE (:keyword IS NULL OR :keyword = ''
                       OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<Tag> findAllWithKeyword(@Param("keyword") String keyword);
}
