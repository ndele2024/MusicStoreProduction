package com.musicstore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TitreRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description,
        @Size(max = 60) String genre,
        @NotNull @Min(1900) @Max(2100) Integer annee,
        String mediaType,
        UUID albumId
) {
}
