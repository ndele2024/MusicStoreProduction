package com.musicstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du stockage des médias.
 *
 * @param location        répertoire racine où sont écrits les fichiers audio et vidéo
 * @param maxFileSizeBytes taille maximale acceptée pour un dépôt
 * @param seedOnStartup   copie les médias de démonstration embarqués au démarrage s'ils sont absents
 */
@ConfigurationProperties(prefix = "musicstore.storage")
public record StorageProperties(
        String location,
        long maxFileSizeBytes,
        boolean seedOnStartup
) {
    public StorageProperties {
        if (location == null || location.isBlank()) {
            location = "./media";
        }
        if (maxFileSizeBytes <= 0) {
            maxFileSizeBytes = 200L * 1024 * 1024;
        }
    }
}
