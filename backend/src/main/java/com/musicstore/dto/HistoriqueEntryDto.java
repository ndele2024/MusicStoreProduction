package com.musicstore.dto;

import java.time.Instant;

public record HistoriqueEntryDto(
        TitreDto titre,
        Instant playedAt
) {
}
