package com.musicstore.exception;

/** Violation d'une règle d'unicité métier (email déjà pris, playlist du même nom, ...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
