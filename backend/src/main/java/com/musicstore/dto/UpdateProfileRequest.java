package com.musicstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String userEmail,
        @Min(1) @Max(130) Integer age,
        @Size(max = 30) String sex,
        @Size(max = 255) String avatar,
        List<String> preferences
) {
}
