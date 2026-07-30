package com.grash.server.domain;

/**
 * Estado de um jogador dentro de uma sala. Mutável e não thread-safe por campo
 * isolado (position é escrita só pela thread do game loop; input é escrito
 * pela thread do websocket do próprio jogador) — não há escrita concorrente
 * no mesmo campo, por isso os campos são apenas {@code volatile}.
 */
public class Player {

    private final String id;
    private final String nickname;
    private volatile PlayerStatus status;

    private volatile double x;
    private volatile double y;

    private volatile boolean moveUp;
    private volatile boolean moveDown;
    private volatile boolean moveLeft;
    private volatile boolean moveRight;

    public Player(String id, String nickname) {
        this.id = id;
        this.nickname = nickname;
        this.status = PlayerStatus.WAITING;
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setInput(boolean up, boolean down, boolean left, boolean right) {
        this.moveUp = up;
        this.moveDown = down;
        this.moveLeft = left;
        this.moveRight = right;
    }

    public boolean isMoveUp() {
        return moveUp;
    }

    public boolean isMoveDown() {
        return moveDown;
    }

    public boolean isMoveLeft() {
        return moveLeft;
    }

    public boolean isMoveRight() {
        return moveRight;
    }
}
