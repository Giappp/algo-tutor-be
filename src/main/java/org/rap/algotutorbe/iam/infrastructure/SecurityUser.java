package org.rap.algotutorbe.iam.infrastructure;

import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.iam.domain.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record SecurityUser(User user) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().getName()));
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPasswordHashed();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

}
