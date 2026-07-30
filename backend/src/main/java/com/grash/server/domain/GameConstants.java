package com.grash.server.domain;

/**
 * Parâmetros da arena de jogo. Espelhados no frontend (game.constants.ts)
 * para o canvas renderizar no mesmo espaço de coordenadas do servidor.
 * O servidor é autoritativo: o cliente só envia input, nunca posição.
 */
public final class GameConstants {

    public static final int ARENA_WIDTH = 800;
    public static final int ARENA_HEIGHT = 600;
    public static final double PLAYER_RADIUS = 16;
    public static final double PLAYER_SPEED_PER_SECOND = 260;
    public static final int TICK_RATE_MS = 50; // 20 ticks/segundo

    private GameConstants() {
    }
}
