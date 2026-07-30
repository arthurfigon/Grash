package com.grash.server.service;

import com.grash.server.domain.Player;
import com.grash.server.domain.PlayerStatus;
import com.grash.server.domain.Room;
import com.grash.server.domain.RoomStatus;
import com.grash.server.exception.PlayerNotFoundException;
import com.grash.server.exception.RoomFullException;
import com.grash.server.exception.RoomNotFoundException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado de todas as salas ativas, em memória (sem banco — ver
 * ARCHITECTURE.md). Uma única instância do backend é assumida; escalar
 * horizontalmente exigiria mover esse estado para Redis.
 */
@Service
public class RoomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem O/0/I/1 ambíguos
    private static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Room> roomsById = new ConcurrentHashMap<>();
    private final Map<String, String> roomIdByCode = new ConcurrentHashMap<>();
    private final WordBankService wordBankService;

    public RoomService(WordBankService wordBankService) {
        this.wordBankService = wordBankService;
    }

    /**
     * @param theme tema fixo escolhido pelo dono pra todas as rodadas; null/vazio
     *              ou um tema desconhecido = sorteia um tema novo a cada rodada.
     */
    public Room createRoom(String nickname, String theme) {
        String roomId = UUID.randomUUID().toString();
        String code = generateUniqueCode();
        Player owner = new Player(UUID.randomUUID().toString(), nickname.trim());

        String fixedTheme = (theme != null && wordBankService.isKnownTheme(theme)) ? theme : null;
        Room room = new Room(roomId, code, owner.getId(), fixedTheme);
        room.addPlayer(owner);

        roomsById.put(roomId, room);
        roomIdByCode.put(code, roomId);
        return room;
    }

    public Player joinRoomByCode(String code, String nickname) {
        Room room = getRoomByCode(code);
        if (room.isFull()) {
            throw new RoomFullException(code);
        }
        Player player = new Player(UUID.randomUUID().toString(), nickname.trim());
        room.addPlayer(player);
        return player;
    }

    public Room getRoomByCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        String roomId = roomIdByCode.get(normalized);
        if (roomId == null) {
            throw new RoomNotFoundException(normalized);
        }
        return getRoomById(roomId);
    }

    public Room getRoomById(String roomId) {
        Room room = roomsById.get(roomId);
        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }
        return room;
    }

    public Player getPlayer(Room room, String playerId) {
        Player player = room.getPlayer(playerId);
        if (player == null) {
            throw new PlayerNotFoundException(playerId);
        }
        return player;
    }

    /**
     * Marca o jogador como pronto/não pronto. Se todos ficarem prontos
     * (mínimo {@link Room#MIN_PLAYERS}), a sala vira IN_PROGRESS — quem
     * chamou isso (RoomWebSocketController) é responsável por então acionar
     * o GameService pra sortear a primeira rodada.
     */
    public Room setReady(String roomId, String playerId, boolean ready) {
        Room room = getRoomById(roomId);
        Player player = getPlayer(room, playerId);
        player.setStatus(ready ? PlayerStatus.READY : PlayerStatus.WAITING);

        if (room.getStatus() == RoomStatus.WAITING && room.allReady()) {
            room.setStatus(RoomStatus.IN_PROGRESS);
        }
        return room;
    }

    /**
     * Remove o jogador da sala. Se a sala ficar vazia, ela é destruída
     * (código liberado para reuso).
     */
    public void leaveRoom(String roomId, String playerId) {
        Room room = roomsById.get(roomId);
        if (room == null) {
            return;
        }
        room.removePlayer(playerId);
        if (room.isEmpty()) {
            roomsById.remove(roomId);
            roomIdByCode.remove(room.getCode());
        }
    }

    public Collection<Room> getAllRooms() {
        return roomsById.values();
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generateCode();
        } while (roomIdByCode.containsKey(code));
        return code;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
