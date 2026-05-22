package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.Testcase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestcaseRepository extends JpaRepository<Testcase, Long> {
    @Query("SELECT t FROM Testcase t WHERE t.codingLesson.id = :lessonId ORDER BY t.sortOrder")
    Optional<List<Testcase>> findByLessonIdOrderByOrderIndex(@Param("lessonId") Long lessonId);

    @Query("SELECT t FROM Testcase t WHERE t.codingLesson.id = :lessonId AND t.isSample = true ORDER BY t.sortOrder")
    List<Testcase> findSamplesByLessonId(@Param("lessonId") Long lessonId);

    @Modifying
    @Query("DELETE FROM Testcase t WHERE t.codingLesson.id = :lessonId")
    void deleteAllByLessonId(@Param("lessonId") Long lessonId);

    List<Testcase> findByCodingLessonId(Long lessonId);
}
