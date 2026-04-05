package org.rap.algotutorbe.iam.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    List<RefreshToken> sessions = new ArrayList<>();
    @Column(nullable = false, unique = true)
    private String userName;
    @Column(nullable = false, unique = true)
    private String email;
    private String passwordHashed;
    private Integer totalSolved;

    private boolean isEnabled;
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
