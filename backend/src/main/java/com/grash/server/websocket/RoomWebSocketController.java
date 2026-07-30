package com.grash.server.websocket;

import com.grash.server.domain.Room;
import com.grash.server.domain.RoomStatus;
import com.grash.server.dto.ClueMessage;
import com.grash.server.dto.GuessMessage;
import com.grash.server.dto.JoinSocketMessage;
import com.grash.server.dto.ReadyMessage;
import com.grash.server.dto.RoomStateMessage;
import com.grash.server.dto.VoteMessage;
import com.grash.server.service.GameService;
import com.grash.server.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Canal STOMP de uma sala:
 * - IN  /app/rooms/{roomId}/join   -> associa a sessão websocket ao jogador
 * - IN  /app/rooms/{roomId}/ready  -> marca pronto/não pronto (dispara o jogo quando todos prontos)
 * - IN  /app/rooms/{roomId}/clue   -> dica do jogador da vez, na fase de dicas
 * - IN  /app/rooms/{roomId}/vote   -> voto anônimo em quem acha que é o impostor (impostor não vota)
 * - IN  /app/rooms/{roomId}/guess  -> palpite do impostor sobre a palavra secreta
 * - OUT /topic/rooms/{roomId}      -> snapshot da sala (RoomStateMessage)
 * - OUT /topic/rooms/{roomId}/game -> estado público da rodada (GameStateMessage)
 * - OUT /user/queue/card           -> carta privada do jogador (GameService, por sessão)
 */
@Controller
public class RoomWebSocketController {

    private final RoomService roomService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    public RoomWebSocketController(RoomService roomService,
                                    GameService gameService,
                                    SimpMessagingTemplate messagingTemplate,
                                    WebSocketSessionRegistry sessionRegistry) {
        this.roomService = roomService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    @MessageMapping("/rooms/{roomId}/join")
    public void join(@DestinationVariable String roomId, JoinSocketMessage message, SimpMessageHeaderAccessor headerAccessor) {
        Room room = roomService.getRoomById(roomId);
        roomService.getPlayer(room, message.playerId()); // valida que o jogador existe na sala

        sessionRegistry.register(headerAccessor.getSessionId(), roomId, message.playerId());
        broadcastRoomState(room);
    }

    @MessageMapping("/rooms/{roomId}/ready")
    public void ready(@DestinationVariable String roomId, ReadyMessage message) {
        boolean wasWaiting = roomService.getRoomById(roomId).getStatus() == RoomStatus.WAITING;
        Room room = roomService.setReady(roomId, message.playerId(), message.ready());

        if (wasWaiting && room.getStatus() == RoomStatus.IN_PROGRESS) {
            gameService.startGame(room);
        }
        broadcastRoomState(room);
    }

    @MessageMapping("/rooms/{roomId}/clue")
    public void clue(@DestinationVariable String roomId, ClueMessage message) {
        Room room = roomService.getRoomById(roomId);
        gameService.submitClue(room, message.playerId(), message.text());
    }

    @MessageMapping("/rooms/{roomId}/vote")
    public void vote(@DestinationVariable String roomId, VoteMessage message) {
        Room room = roomService.getRoomById(roomId);
        gameService.submitVote(room, message.playerId(), message.votedForId());
    }

    @MessageMapping("/rooms/{roomId}/guess")
    public void guess(@DestinationVariable String roomId, GuessMessage message) {
        Room room = roomService.getRoomById(roomId);
        gameService.submitImpostorGuess(room, message.playerId(), message.guess());
    }

    private void broadcastRoomState(Room room) {
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), RoomStateMessage.of(room));
    }
}
