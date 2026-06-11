package org.rap.algotutorbe.iam.domain.repositories;

import org.rap.algotutorbe.iam.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("SELECT u from User u where u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u from User u left join fetch u.role where u.username = :username")
    Optional<User> findByUserNameWithRole(@Param("username") String username);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query(
            value = """
                    SELECT u
                    FROM User u
                    LEFT JOIN FETCH u.role
                    WHERE (COALESCE(:search, '') = ''
                        OR LOWER(u.username) LIKE CONCAT('%', :search, '%')
                        OR LOWER(u.email) LIKE CONCAT('%', :search, '%'))
                    """,
            countQuery = """
                    SELECT COUNT(u)
                    FROM User u
                    WHERE (COALESCE(:search, '') = ''
                        OR LOWER(u.username) LIKE CONCAT('%', :search, '%')
                        OR LOWER(u.email) LIKE CONCAT('%', :search, '%'))
                    """
    )
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
