package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.CodingLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodingLessonRepository extends JpaRepository<CodingLesson, Long> {

    @Query("SELECT cl FROM CodingLesson cl LEFT JOIN FETCH cl.testCases WHERE cl.id = :id")
    Optional<CodingLesson> findByIdWithTestCases(@Param("id") Long id);

    @Query("SELECT cl FROM CodingLesson cl LEFT JOIN FETCH cl.testCases WHERE cl.slug = :slug")
    Optional<CodingLesson> findBySlugWithTestCases(@Param("slug") String slug);
}
