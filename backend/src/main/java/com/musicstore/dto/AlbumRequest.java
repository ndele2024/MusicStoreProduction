package com.musicstore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlbumRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull @Min(1900) @Max(2100) Integer annee
) {
}
