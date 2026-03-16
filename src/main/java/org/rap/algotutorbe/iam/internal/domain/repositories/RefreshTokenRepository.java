package org.rap.algotutorbe.iam.internal.domain.repositories;

import org.rap.algotutorbe.iam.internal.domain.model.RefreshToken;
import org.rap.algotutorbe.iam.internal.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(UUID token);

    int deleteByUser(User user);
}
