package com.grash.server.domain;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sala em memória. Todo o estado de uma partida vive aqui enquanto o
 * servidor roda — nada é persistido (ver decisão em ARCHITECTURE.md: sem
 * banco no MVP). Se o processo reiniciar, todas as salas somem.
 */
public class Room {

    public static final int MAX_PLAYERS = 8;

    private final String id;
    private final String code;
    private final String ownerId;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private volatile RoomStatus status = RoomStatus.WAITING;

    public Room(String id, String code, String ownerId) {
        this.id = id;
        this.code = code;
        this.ownerId = ownerId;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public Collection<Player> getPlayers() {
        return players.values();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public boolean allReady() {
        if (players.size() < 2) {
            return false;
        }
        return players.values().stream().allMatch(p -> p.getStatus() == PlayerStatus.READY);
    }
}
