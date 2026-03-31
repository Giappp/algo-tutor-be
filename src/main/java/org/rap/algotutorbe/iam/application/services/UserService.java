package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
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
