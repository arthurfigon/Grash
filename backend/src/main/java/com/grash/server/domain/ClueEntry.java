package com.grash.server.domain;

/** Uma dica dada por um jogador, numa das 3 voltas de dicas da rodada. */
public record ClueEntry(String playerId, int lap, String text) {
}
