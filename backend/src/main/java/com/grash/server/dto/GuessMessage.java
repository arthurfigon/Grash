package com.grash.server.dto;

/** Palpite do impostor sobre a palavra secreta da rodada. */
public record GuessMessage(String playerId, String guess) {
}
