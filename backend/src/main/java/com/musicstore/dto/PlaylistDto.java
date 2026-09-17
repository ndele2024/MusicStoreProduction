package com.musicstore.dto;

import java.util.List;
import java.util.UUID;

public record PlaylistDto(
        UUID id,
        String nom,
        List<TitreDto> titres
) {
}
