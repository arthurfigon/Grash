package com.grash.server.domain;

public enum RoundPhase {
    /** Jogadores dão dicas em turnos, 3 voltas completas na mesma ordem. */
    CLUE_GIVING,
    /** Todo mundo já deu as 3 dicas — hora de votar em quem acha que é o impostor. */
    VOTING,
    /** Todos votaram — impostor revelado, pontos distribuídos, pausa antes da próxima rodada. */
    REVEAL
}
