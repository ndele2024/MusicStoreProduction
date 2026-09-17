package com.musicstore.exception;

/** L'utilisateur est authentifié mais n'est pas propriétaire de la ressource visée. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
