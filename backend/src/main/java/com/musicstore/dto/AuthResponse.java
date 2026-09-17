package com.musicstore.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserDto user
) {
}
