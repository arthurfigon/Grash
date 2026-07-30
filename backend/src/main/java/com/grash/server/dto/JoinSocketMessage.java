package com.grash.server.dto;

/**
 * Primeira mensagem enviada pelo cliente após abrir a conexão STOMP,
 * associando a sessão de websocket ao jogador já criado via REST
 * (POST /api/rooms ou /api/rooms/{code}/join).
 */
public record JoinSocketMessage(String playerId) {
}
