package com.grash.server.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mapeia sessão STOMP <-> (sala, jogador), nos dois sentidos. Necessário
 * porque a conexão websocket é aberta separadamente da chamada REST que
 * criou o jogador — essa associação permite (a) limpar a sala quando a aba
 * fecha, sem o cliente precisar avisar, e (b) mandar mensagem privada pra
 * sessão de um jogador específico (ver GameService — a "carta" de cada
 * rodada), sabendo só o playerId.
 */
@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(String roomId, String playerId) {
    }

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdByPlayerId = new ConcurrentHashMap<>();

    public void register(String sessionId, String roomId, String playerId) {
        sessions.put(sessionId, new SessionInfo(roomId, playerId));
        sessionIdByPlayerId.put(playerId, sessionId);
    }

    public SessionInfo remove(String sessionId) {
        SessionInfo info = sessions.remove(sessionId);
        if (info != null) {
            sessionIdByPlayerId.remove(info.playerId(), sessionId);
        }
        return info;
    }

    /** @return a sessão STOMP atual do jogador, ou {@code null} se ele não estiver conectado. */
    public String findSessionId(String playerId) {
        return sessionIdByPlayerId.get(playerId);
    }
}
