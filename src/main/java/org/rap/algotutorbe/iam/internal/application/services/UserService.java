package org.rap.algotutorbe.iam.internal.application.services;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.rap.algotutorbe.iam.SecurityUser;
import org.rap.algotutorbe.iam.internal.domain.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public SecurityUser loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(SecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("Email not found"));
    }
}
