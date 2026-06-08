package org.rap.algotutorbe.iam.domain.repositories;

import org.rap.algotutorbe.iam.domain.model.Role;
import org.rap.algotutorbe.iam.domain.model.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByCode(RoleCode code);

    Optional<Role> findByCode(RoleCode code);
}
