package com.musicstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.musicstore.domain.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Génère et vérifie les jetons d'accès signés en HS256. */
@Service
public class JwtService {

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = buildKey(properties.secret());
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("musicstore.jwt.secret doit être défini");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException | IllegalArgumentException notBase64) {
            // La cle peut aussi etre fournie en texte brut : on prend alors ses octets UTF-8.
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("musicstore.jwt.secret doit faire au moins 32 octets");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.expiration());
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(properties.issuer())
                .claims(Map.of(
                        "email", user.getUserEmail(),
                        "role", user.getRole().name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public long expirationSeconds() {
        return properties.expiration();
    }

    /** Retourne l'identifiant du porteur, ou {@code Optional.empty()} si le jeton est invalide ou expiré. */
    public Optional<UUID> extractUserId(String token) {
        return parse(token).map(claims -> {
            try {
                return UUID.fromString(claims.getSubject());
            } catch (IllegalArgumentException malformedSubject) {
                return null;
            }
        });
    }

    public Optional<Claims> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }
}
