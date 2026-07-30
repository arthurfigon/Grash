package com.grash.server.exception;

public class RoomFullException extends RuntimeException {
    public RoomFullException(String code) {
        super("Sala cheia: " + code);
    }
}
