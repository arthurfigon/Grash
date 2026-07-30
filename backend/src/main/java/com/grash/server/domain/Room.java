package com.grash.server.domain;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sala em memória — todo o estado (jogadores, rodada atual) vive aqui
 * enquanto o processo roda. Sem banco de dados (ver ARCHITECTURE.md):
 * reiniciar o servidor apaga as salas ativas, decisão deliberada pra manter
 * o projeto simples.
 */
public class Room {

    public static final int MIN_PLAYERS = 3;
    public static final int MAX_PLAYERS = 8;

    private final String id;
    private final String code;
    private final String ownerId;
    /** Tema fixo escolhido pelo dono ao criar a sala; null = sorteia um tema novo a cada rodada. */
    private final String fixedTheme;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private volatile RoomStatus status = RoomStatus.WAITING;
    private volatile RoundState currentRound;

    public Room(String id, String code, String ownerId, String fixedTheme) {
        this.id = id;
        this.code = code;
        this.ownerId = ownerId;
        this.fixedTheme = fixedTheme;
    }

    public String getFixedTheme() {
        return fixedTheme;
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

    public RoundState getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(RoundState currentRound) {
        this.currentRound = currentRound;
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
        if (players.size() < MIN_PLAYERS) {
            return false;
        }
        return players.values().stream().allMatch(p -> p.getStatus() == PlayerStatus.READY);
    }
}
