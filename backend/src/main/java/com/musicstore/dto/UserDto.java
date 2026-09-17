package com.musicstore.dto;

import java.util.List;
import java.util.UUID;

/** Profil utilisateur : ne contient jamais le mot de passe, même haché. */
public record UserDto(
        UUID id,
        String fullName,
        Integer age,
        String sex,
        String userName,
        String userEmail,
        String role,
        String avatar,
        List<String> preferences
) {
}
