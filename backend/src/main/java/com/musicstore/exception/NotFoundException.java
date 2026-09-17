package com.musicstore.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entite, Object id) {
        return new NotFoundException(entite + " introuvable : " + id);
    }
}
