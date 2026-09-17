package com.musicstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origines autorisées à appeler l'API depuis un navigateur.
 *
 * <p>Les valeurs sont des motifs : le serveur de développement Angular peut changer de port,
 * et le proxy de « ng serve » transmet l'origine du navigateur telle quelle. En production,
 * définir des origines exactes via {@code MUSICSTORE_CORS_ORIGINS}.
 */
@ConfigurationProperties(prefix = "musicstore.cors")
public record CorsProperties(List<String> allowedOrigins) {
    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:[*]", "http://127.0.0.1:[*]");
        }
    }
}
