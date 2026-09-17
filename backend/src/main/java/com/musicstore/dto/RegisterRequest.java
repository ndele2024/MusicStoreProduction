package com.musicstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Min(1) @Max(130) Integer age,
        @Size(max = 30) String sex,
        @NotBlank @Size(min = 3, max = 60) String userName,
        @NotBlank @Email @Size(max = 180) String userEmail,
        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[^A-Za-z]).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre ou caractère spécial")
        String password,
        String role
) {
}
