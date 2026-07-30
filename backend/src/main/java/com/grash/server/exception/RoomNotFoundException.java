package com.grash.server.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String code) {
        super("Sala não encontrada: " + code);
    }
}
