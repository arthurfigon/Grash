package com.grash.server.websocket;

import com.grash.server.domain.Room;
import com.grash.server.dto.InputMessage;
import com.grash.server.dto.JoinSocketMessage;
import com.grash.server.dto.ReadyMessage;
import com.grash.server.dto.RoomStateMessage;
import com.grash.server.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Canal STOMP de uma sala:
 * - IN  /app/rooms/{roomId}/join   -> associa a sessão websocket ao jogador
 * - IN  /app/rooms/{roomId}/ready  -> marca pronto/não pronto
 * - IN  /app/rooms/{roomId}/input  -> teclas pressionadas (jogo em andamento)
 * - OUT /topic/rooms/{roomId}      -> snapshot da sala (RoomStateMessage)
 * - OUT /topic/rooms/{roomId}/game -> posições do tick atual (GameLoopService)
 */
@Controller
public class RoomWebSocketController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    public RoomWebSocketController(RoomService roomService,
                                    SimpMessagingTemplate messagingTemplate,
                                    WebSocketSessionRegistry sessionRegistry) {
        this.roomService = roomService;
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
        Room room = roomService.setReady(roomId, message.playerId(), message.ready());
        broadcastRoomState(room);
    }

    @MessageMapping("/rooms/{roomId}/input")
    public void input(@DestinationVariable String roomId, InputMessage message) {
        roomService.applyInput(roomId, message.playerId(), message.up(), message.down(), message.left(), message.right());
    }

    private void broadcastRoomState(Room room) {
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), RoomStateMessage.of(room));
    }
}
