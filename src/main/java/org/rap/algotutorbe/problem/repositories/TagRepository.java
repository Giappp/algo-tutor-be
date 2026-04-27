package org.rap.algotutorbe.problem.repositories;

import org.rap.algotutorbe.problem.domain.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    @Query("""
                SELECT t FROM Tag t
                WHERE (:keyword IS NULL OR :keyword = ''
                       OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<Tag> findAllWithKeyword(@Param("keyword") String keyword);

    Optional<Tag> findByNameIgnoreCase(String name);

    @Query("SELECT t FROM Tag t LEFT JOIN FETCH t.problems WHERE t.slug = :slug")
    Optional<Tag> findBySlugWithProblems(@Param("slug") String slug);
}
