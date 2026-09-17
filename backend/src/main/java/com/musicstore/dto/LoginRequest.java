package com.musicstore.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String userEmail,
        @NotBlank String password
) {
}
