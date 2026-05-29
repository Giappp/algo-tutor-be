package org.rap.algotutorbe.iam.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refreshTokens", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
})
@Getter
@Setter
public class RefreshToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, unique = true)
    private UUID token;
    private Instant expiryDate;
    private String ipv4Address;
    private String deviceInfo;

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}
