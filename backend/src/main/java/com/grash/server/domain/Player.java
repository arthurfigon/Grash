package com.grash.server.domain;

/**
 * Jogador dentro de uma sala — tudo em memória (sem banco, ver
 * ARCHITECTURE.md). {@code score} acumula ao longo das 10 rodadas do jogo
 * do Impostor.
 */
public class Player {

    private final String id;
    private final String nickname;
    private volatile PlayerStatus status;
    private volatile int score;

    public Player(String id, String nickname) {
        this.id = id;
        this.nickname = nickname;
        this.status = PlayerStatus.WAITING;
        this.score = 0;
    }

    public String getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void addPoint() {
        this.score++;
    }
}
