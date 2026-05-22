package org.rap.algotutorbe.iam.infrastructure;

import lombok.Getter;
import org.rap.algotutorbe.iam.domain.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SecurityUser implements UserDetails {
    @Getter
    private final transient User user;

    @Getter
    private final UUID id;
    private final String username;
    @Getter
    private final String email;
    @Getter
    private final String roleCode;
    private final String passwordHashed;

    private final boolean enabled;

    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.user = user;
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.enabled = user.isEnabled();
        this.passwordHashed = user.getPasswordHashed();

        this.roleCode = user.getRole().getCode().name();
        Set<GrantedAuthority> auths = new HashSet<>();
        auths.add(new SimpleGrantedAuthority("ROLE_" + this.roleCode));

        this.authorities = auths;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHashed;
    }


    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }


}
