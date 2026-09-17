package com.musicstore.dto;

import java.util.List;
import java.util.UUID;

/** Le champ {@code titres} porte les identifiants des titres, comme attendu par le modèle Angular. */
public record AlbumDto(
        UUID id,
        String name,
        Integer annee,
        List<UUID> titres,
        UUID artisteId,
        String artisteNom
) {
}
