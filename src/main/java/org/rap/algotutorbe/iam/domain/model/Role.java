package org.rap.algotutorbe.iam.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends BaseEntity {
    @Column(name = "code", nullable = false, unique = true, length = 50)
    @Enumerated(EnumType.STRING)
    private RoleCode code;
    @Column(unique = true)
    private String name;
    private String description;
}
