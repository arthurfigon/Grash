package com.grash.server.domain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Estado de uma rodada do jogo do Impostor (1 de 10). {@code secretWord} e
 * {@code impostorId} nunca são expostos no broadcast público — só via
 * mensagem privada por sessão (ver GameService/RoomWebSocketController).
 */
public class RoundState {

    private final int roundNumber;
    private final String theme;
    private final String secretWord;
    private final String impostorId;
    private final List<String> turnOrder;

    private volatile RoundPhase phase = RoundPhase.CLUE_GIVING;
    private volatile int currentTurnIndex = 0;
    private volatile int clueLap = 1;
    private final List<ClueEntry> clues = new CopyOnWriteArrayList<>();
    /** Votos dos jogadores NÃO-impostores só — o impostor não vota mais. */
    private final Map<String, String> votes = new ConcurrentHashMap<>();
    private volatile Map<String, Integer> scoreDeltas = Map.of();

    /** Palpite do impostor sobre a palavra da rodada — null até ele responder. */
    private volatile String impostorGuess;
    private volatile boolean impostorGuessedCorrectly;

    public RoundState(int roundNumber, String theme, String secretWord, String impostorId, List<String> turnOrder) {
        this.roundNumber = roundNumber;
        this.theme = theme;
        this.secretWord = secretWord;
        this.impostorId = impostorId;
        this.turnOrder = List.copyOf(turnOrder);
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public String getTheme() {
        return theme;
    }

    public String getSecretWord() {
        return secretWord;
    }

    public String getImpostorId() {
        return impostorId;
    }

    public List<String> getTurnOrder() {
        return turnOrder;
    }

    public RoundPhase getPhase() {
        return phase;
    }

    public void setPhase(RoundPhase phase) {
        this.phase = phase;
    }

    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public int getClueLap() {
        return clueLap;
    }

    public String currentTurnPlayerId() {
        return turnOrder.get(currentTurnIndex);
    }

    public boolean isPlayersTurn(String playerId) {
        return currentTurnPlayerId().equals(playerId);
    }

    public List<ClueEntry> getClues() {
        return clues;
    }

    public void recordClue(String playerId, String text) {
        clues.add(new ClueEntry(playerId, clueLap, text));
    }

    /** Avança pro próximo jogador; ao completar uma volta, incrementa a "volta" (clueLap). */
    public void advanceTurn() {
        currentTurnIndex++;
        if (currentTurnIndex >= turnOrder.size()) {
            currentTurnIndex = 0;
            clueLap++;
        }
    }

    public boolean isClueGivingComplete() {
        return clueLap > GameConstants.CLUE_LAPS_PER_ROUND;
    }

    public Map<String, String> getVotes() {
        return votes;
    }

    public boolean hasVoted(String playerId) {
        return votes.containsKey(playerId);
    }

    public void recordVote(String voterId, String votedForId) {
        votes.put(voterId, votedForId);
    }

    public boolean allVoted(int totalPlayers) {
        return votes.size() >= totalPlayers;
    }

    public Map<String, Integer> getScoreDeltas() {
        return scoreDeltas;
    }

    public void setScoreDeltas(Map<String, Integer> scoreDeltas) {
        this.scoreDeltas = scoreDeltas;
    }

    public String getImpostorGuess() {
        return impostorGuess;
    }

    public void setImpostorGuess(String impostorGuess) {
        this.impostorGuess = impostorGuess;
    }

    public boolean hasImpostorGuessed() {
        return impostorGuess != null;
    }

    public boolean isImpostorGuessedCorrectly() {
        return impostorGuessedCorrectly;
    }

    public void setImpostorGuessedCorrectly(boolean impostorGuessedCorrectly) {
        this.impostorGuessedCorrectly = impostorGuessedCorrectly;
    }
}
