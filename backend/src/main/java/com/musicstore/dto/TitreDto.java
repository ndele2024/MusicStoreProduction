package com.musicstore.dto;

import java.util.UUID;

/**
 * Représentation d'un titre exposée au frontend.
 *
 * @param maNote note donnée par l'utilisateur courant, {@code null} s'il n'a pas voté ou n'est pas connecté
 */
public record TitreDto(
        UUID id,
        String name,
        String description,
        String genre,
        Integer annee,
        String nomFichier,
        String nomImage,
        String mediaType,
        String mediaUrl,
        long vues,
        double noteMoyenne,
        int nombreNotes,
        Integer maNote,
        UUID artisteId,
        String artisteNom,
        UUID albumId,
        String albumNom
) {
}
