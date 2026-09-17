package com.musicstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Lit le jeton porteur et alimente le contexte de sécurité.
 *
 * <p>Un jeton absent ou invalide laisse la requête anonyme : c'est la chaîne d'autorisation qui
 * décide ensuite si l'accès est permis, ce qui garde les endpoints publics accessibles.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    /** Certains lecteurs média ne peuvent pas poser d'en-tête : le streaming accepte aussi ?token=. */
    private static final String TOKEN_QUERY_PARAM = "token";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveToken(request)
                    .flatMap(jwtService::extractUserId)
                    .ifPresent(userId -> authenticate(userId, request));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(UUID userId, HttpServletRequest request) {
        try {
            UserDetails details = userDetailsService.loadUserById(userId);
            var authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException deletedUser) {
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
        }
        String param = request.getParameter(TOKEN_QUERY_PARAM);
        if (param != null && !param.isBlank()) {
            return Optional.of(param.trim());
        }
        return Optional.empty();
    }
}
