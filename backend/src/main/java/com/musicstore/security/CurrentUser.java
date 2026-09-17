package com.musicstore.security;

import com.musicstore.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Accès à l'utilisateur authentifié depuis la couche service. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<User> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AppUserDetails details) {
            return Optional.of(details.getUser());
        }
        return Optional.empty();
    }
}
