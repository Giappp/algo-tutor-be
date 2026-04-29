package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.Editorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EditorialRepository extends JpaRepository<Editorial, Long> {
}
