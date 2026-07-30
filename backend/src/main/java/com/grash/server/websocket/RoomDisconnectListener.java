package com.grash.server.websocket;

import com.grash.server.domain.Room;
import com.grash.server.dto.RoomStateMessage;
import com.grash.server.exception.RoomNotFoundException;
import com.grash.server.service.RoomService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class RoomDisconnectListener {

    private final WebSocketSessionRegistry sessionRegistry;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomDisconnectListener(WebSocketSessionRegistry sessionRegistry,
                                   RoomService roomService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.sessionRegistry = sessionRegistry;
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = SimpMessageHeaderAccessor.wrap(event.getMessage()).getSessionId();
        WebSocketSessionRegistry.SessionInfo info = sessionRegistry.remove(sessionId);
        if (info == null) {
            return;
        }

        roomService.leaveRoom(info.roomId(), info.playerId());

        try {
            Room room = roomService.getRoomById(info.roomId());
            messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), RoomStateMessage.of(room));
        } catch (RoomNotFoundException ex) {
            // sala foi removida por ficar vazia — nada para broadcastar
        }
    }
}
