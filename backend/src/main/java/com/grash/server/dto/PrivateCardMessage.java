package com.grash.server.dto;

/**
 * A "carta" que só um jogador específico vê — enviada pela fila privada por
 * sessão (/user/queue/card), nunca no broadcast público da sala. Se
 * {@code impostor} for true, {@code word} vem nulo (o cliente mostra a
 * carta vermelha "Impostor"); senão mostra a carta verde com {@code word}.
 */
public record PrivateCardMessage(String theme, boolean impostor, String word) {
}
