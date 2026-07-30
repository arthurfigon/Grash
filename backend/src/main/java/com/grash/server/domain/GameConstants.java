package com.grash.server.domain;

/**
 * Parâmetros do jogo do Impostor.
 */
public final class GameConstants {

    public static final int TOTAL_ROUNDS = 10;
    public static final int CLUE_LAPS_PER_ROUND = 3;
    /** Pausa (segundos) na fase de revelação antes de começar a próxima rodada automaticamente. */
    public static final int REVEAL_DURATION_SECONDS = 8;

    public static final int CORRECT_VOTE_POINTS = 1;
    /** Pontos que o impostor ganha pra CADA jogador que vota errado. */
    public static final int WRONG_VOTE_IMPOSTOR_POINTS = 2;
    /** Bônus extra se o impostor acertar a palavra da rodada no palpite final. */
    public static final int IMPOSTOR_CORRECT_GUESS_BONUS = 3;

    private GameConstants() {
    }
}
