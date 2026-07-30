package com.grash.server.dto;

import com.grash.server.domain.Room;

import java.util.List;

/**
 * Snapshot completo de uma sala. Usado tanto como resposta REST (criar/entrar/consultar)
 * quanto como payload broadcastado em /topic/rooms/{roomId} sempre que a
 * composição/estado da sala muda (entrada, saída, ready, início de jogo).
 */
public record RoomStateMessage(
        String roomId,
        String code,
        String status,
        String ownerId,
        /** Tema fixo escolhido na criação da sala; null = sorteia um tema novo a cada rodada. */
        String fixedTheme,
        List<PlayerDto> players,
        String requestingPlayerId
) {

    public static RoomStateMessage of(Room room) {
        return of(room, null);
    }

    public static RoomStateMessage of(Room room, String requestingPlayerId) {
        List<PlayerDto> players = room.getPlayers().stream().map(PlayerDto::from).toList();
        return new RoomStateMessage(
                room.getId(),
                room.getCode(),
                room.getStatus().name(),
                room.getOwnerId(),
                room.getFixedTheme(),
                players,
                requestingPlayerId
        );
    }
}
