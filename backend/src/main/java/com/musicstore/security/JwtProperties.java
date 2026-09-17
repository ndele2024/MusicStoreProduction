package com.musicstore.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret     clé HMAC en Base64 ou en texte brut, d'au moins 32 octets
 * @param expiration durée de validité du jeton, en secondes
 * @param issuer     émetteur inscrit dans le jeton et vérifié à la lecture
 */
@ConfigurationProperties(prefix = "musicstore.jwt")
public record JwtProperties(
        String secret,
        long expiration,
        String issuer
) {
    public JwtProperties {
        if (expiration <= 0) {
            expiration = 43_200L;
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "musicstore";
        }
    }
}
