package com.musicstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistRequest(
        @NotBlank @Size(max = 120) String nom
) {
}
