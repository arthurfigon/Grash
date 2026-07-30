package com.grash.server.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String playerId) {
        super("Jogador não encontrado: " + playerId);
    }
}
