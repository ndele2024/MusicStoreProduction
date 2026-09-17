package com.musicstore.security;

import com.musicstore.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByUserEmailIgnoreCase(email)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        return userRepository.findById(id)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + id));
    }
}
