package com.musicstore.domain;

/** Rôles applicatifs. Le préfixe ROLE_ est ajouté par {@code SecurityConfig} au moment de l'autorisation. */
public enum Role {
    AUDITEUR,
    ARTISTE,
    ADMIN;

    /** Valeur exposée au frontend (minuscules), symétrique de {@link #fromApi(String)}. */
    public String toApi() {
        return name().toLowerCase();
    }

    public static Role fromApi(String value) {
        if (value == null || value.isBlank()) {
            return AUDITEUR;
        }
        return switch (value.trim().toLowerCase()) {
            case "artiste", "artist" -> ARTISTE;
            case "admin" -> ADMIN;
            default -> AUDITEUR;
        };
    }
}
