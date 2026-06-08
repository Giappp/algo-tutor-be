package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.Editorial;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EditorialRepository extends JpaRepository<Editorial, Long> {

    List<Editorial> findByCodingLessonId(Long codingLessonId);

    Optional<Editorial> findByCodingLessonIdAndLanguage(Long codingLessonId, ProgrammingLanguage language);
}
