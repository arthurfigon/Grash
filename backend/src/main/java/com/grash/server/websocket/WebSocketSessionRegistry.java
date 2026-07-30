package com.grash.server.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mapeia sessão STOMP -> (sala, jogador). Necessário porque a conexão
 * websocket é aberta separadamente da chamada REST que criou o jogador;
 * é essa associação que permite limpar a sala quando a aba fecha
 * (SessionDisconnectEvent) sem o cliente precisar avisar explicitamente.
 */
@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(String roomId, String playerId) {
    }

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, String roomId, String playerId) {
        sessions.put(sessionId, new SessionInfo(roomId, playerId));
    }

    public SessionInfo remove(String sessionId) {
        return sessions.remove(sessionId);
    }
}
