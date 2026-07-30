package com.grash.server.service;

import com.grash.server.domain.GameConstants;
import com.grash.server.domain.Player;
import com.grash.server.domain.Room;
import com.grash.server.domain.RoomStatus;
import com.grash.server.domain.RoundPhase;
import com.grash.server.domain.RoundState;
import com.grash.server.dto.GameStateMessage;
import com.grash.server.dto.PrivateCardMessage;
import com.grash.server.dto.RoomStateMessage;
import com.grash.server.websocket.WebSocketSessionRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Orquestra o jogo do Impostor: sorteio de tema/palavra/impostor/ordem de
 * turnos, dicas, votação, palpite do impostor, revelação e pontuação, e o
 * avanço automático entre as 10 rodadas. Tudo em memória (ver
 * ARCHITECTURE.md) — opera diretamente sobre o {@link Room}/{@link RoundState}
 * que o chamador já tem em mãos, sem depender do RoomService (evita
 * acoplamento circular).
 *
 * Cada jogador recebe sua "carta" (palavra ou "Impostor") por uma fila STOMP
 * privada por sessão — nunca pelo broadcast público da sala, que só carrega
 * informação que todo mundo pode ver (tema, de quem é a vez, dicas dadas).
 */
@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final int MAX_TEXT_LENGTH = 200;

    private final WordBankService wordBankService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Rotação justa do papel de impostor por sala — ver {@link #pickImpostor}. */
    private final Map<String, RotationState> rotations = new ConcurrentHashMap<>();

    public GameService(WordBankService wordBankService, SimpMessagingTemplate messagingTemplate,
                        WebSocketSessionRegistry sessionRegistry) {
        this.wordBankService = wordBankService;
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    public void startGame(Room room) {
        rotations.remove(room.getId());
        startRound(room, 1);
    }

    public void submitClue(Room room, String playerId, String text) {
        RoundState round = room.getCurrentRound();
        if (round == null || round.getPhase() != RoundPhase.CLUE_GIVING || !round.isPlayersTurn(playerId)) {
            return;
        }
        String trimmed = capText(text);
        if (trimmed.isEmpty()) {
            return;
        }

        round.recordClue(playerId, trimmed);
        round.advanceTurn();
        if (round.isClueGivingComplete()) {
            round.setPhase(RoundPhase.VOTING);
        }
        broadcastGameState(room);
    }

    /** Voto de um jogador NÃO-impostor em quem ele acha que é o impostor. */
    public void submitVote(Room room, String playerId, String votedForId) {
        RoundState round = room.getCurrentRound();
        if (round == null || round.getPhase() != RoundPhase.VOTING || round.hasVoted(playerId)) {
            return;
        }
        if (playerId.equals(round.getImpostorId())) {
            return; // impostor não vota
        }
        if (playerId.equals(votedForId) || room.getPlayer(votedForId) == null) {
            return;
        }

        round.recordVote(playerId, votedForId);
        checkVotingComplete(room, round);
    }

    /** Palpite do impostor sobre a palavra secreta da rodada. */
    public void submitImpostorGuess(Room room, String playerId, String guess) {
        RoundState round = room.getCurrentRound();
        if (round == null || round.getPhase() != RoundPhase.VOTING || round.hasImpostorGuessed()) {
            return;
        }
        if (!playerId.equals(round.getImpostorId())) {
            return;
        }
        String trimmed = capText(guess);
        if (trimmed.isEmpty()) {
            return;
        }

        round.setImpostorGuess(trimmed);
        checkVotingComplete(room, round);
    }

    private void checkVotingComplete(Room room, RoundState round) {
        int expectedVoters = room.getPlayers().size() - 1; // todos menos o impostor
        if (round.allVoted(expectedVoters) && round.hasImpostorGuessed()) {
            revealAndScore(room, round);
        } else {
            broadcastGameState(room);
        }
    }

    private void startRound(Room room, int roundNumber) {
        List<Player> players = new ArrayList<>(room.getPlayers());
        List<String> playerIds = players.stream().map(Player::getId).toList();

        List<String> turnOrder = new ArrayList<>(playerIds);
        Collections.shuffle(turnOrder, random);

        String impostorId = pickImpostor(room.getId(), playerIds);
        String fixedTheme = room.getFixedTheme();
        WordBankService.ThemeWord themeWord = fixedTheme != null
                ? new WordBankService.ThemeWord(fixedTheme, wordBankService.pickRandomWordFromTheme(fixedTheme))
                : wordBankService.pickRandom();

        RoundState round = new RoundState(roundNumber, themeWord.theme(), themeWord.word(), impostorId, turnOrder);
        room.setCurrentRound(round);

        for (Player player : players) {
            boolean isImpostor = player.getId().equals(impostorId);
            PrivateCardMessage card = new PrivateCardMessage(themeWord.theme(), isImpostor, isImpostor ? null : themeWord.word());
            sendPrivate(player.getId(), "/queue/card", card);
        }

        broadcastGameState(room);
    }

    /**
     * Sorteia o impostor da rodada distribuindo o papel o mais igualmente
     * possível: embaralha a lista de jogadores uma vez e consome em ordem;
     * quando "dá a volta" (ou o grupo de jogadores muda), embaralha de novo.
     * Assim, ao longo de 10 rodadas, ninguém fica de fora nem é impostor
     * toda hora só por azar do sorteio puro.
     */
    private String pickImpostor(String roomId, List<String> playerIds) {
        RotationState state = rotations.computeIfAbsent(roomId, id -> new RotationState());
        Set<String> currentSet = new HashSet<>(playerIds);

        if (state.index >= state.order.size() || !new HashSet<>(state.order).equals(currentSet)) {
            List<String> shuffled = new ArrayList<>(playerIds);
            Collections.shuffle(shuffled, random);
            state.order = shuffled;
            state.index = 0;
        }

        String impostorId = state.order.get(state.index);
        state.index++;
        return impostorId;
    }

    /**
     * Pontuação da rodada. Duas situações:
     * <ul>
     *   <li>Impostor <b>erra</b> o palpite: pontuação normal da votação —
     *       quem votou certo ganha {@link GameConstants#CORRECT_VOTE_POINTS};
     *       pra cada voto errado, o impostor ganha
     *       {@link GameConstants#WRONG_VOTE_IMPOSTOR_POINTS}.</li>
     *   <li>Impostor <b>acerta</b> o palpite: ninguém que votou certo ganha
     *       ponto (o impostor "rouba" a rodada) — o impostor ainda ganha os
     *       pontos de voto errado normalmente, MAIS um bônus de
     *       {@link GameConstants#IMPOSTOR_CORRECT_GUESS_BONUS}.</li>
     * </ul>
     */
    private void revealAndScore(Room room, RoundState round) {
        round.setPhase(RoundPhase.REVEAL);

        Player impostor = room.getPlayer(round.getImpostorId());
        boolean guessedCorrectly = impostor != null && wordsMatch(round.getImpostorGuess(), round.getSecretWord());
        round.setImpostorGuessedCorrectly(guessedCorrectly);

        Map<String, Integer> deltas = new HashMap<>();
        for (Player player : room.getPlayers()) {
            if (player.getId().equals(round.getImpostorId())) {
                continue;
            }
            String votedFor = round.getVotes().get(player.getId());
            boolean votedCorrectly = round.getImpostorId().equals(votedFor);

            if (votedCorrectly) {
                if (!guessedCorrectly) {
                    player.addPoint();
                    deltas.merge(player.getId(), GameConstants.CORRECT_VOTE_POINTS, Integer::sum);
                }
                // impostor acertou a palavra -> quem votou certo não ganha nada
            } else if (impostor != null) {
                for (int i = 0; i < GameConstants.WRONG_VOTE_IMPOSTOR_POINTS; i++) {
                    impostor.addPoint();
                }
                deltas.merge(impostor.getId(), GameConstants.WRONG_VOTE_IMPOSTOR_POINTS, Integer::sum);
            }
        }

        if (guessedCorrectly && impostor != null) {
            for (int i = 0; i < GameConstants.IMPOSTOR_CORRECT_GUESS_BONUS; i++) {
                impostor.addPoint();
            }
            deltas.merge(impostor.getId(), GameConstants.IMPOSTOR_CORRECT_GUESS_BONUS, Integer::sum);
        }

        round.setScoreDeltas(deltas);

        broadcastGameState(room);
        broadcastRoomState(room);

        scheduler.schedule(() -> advanceOrEndGame(room), GameConstants.REVEAL_DURATION_SECONDS, TimeUnit.SECONDS);
    }

    private void advanceOrEndGame(Room room) {
        RoundState finished = room.getCurrentRound();
        if (finished == null || room.getStatus() != RoomStatus.IN_PROGRESS) {
            return; // sala foi removida ou encerrada nesse meio tempo
        }

        int next = finished.getRoundNumber() + 1;
        if (next > GameConstants.TOTAL_ROUNDS || room.getPlayers().size() < Room.MIN_PLAYERS) {
            room.setStatus(RoomStatus.FINISHED);
            room.setCurrentRound(null);
            broadcastRoomState(room);
            return;
        }
        startRound(room, next);
    }

    /**
     * Compara a palavra chutada com a palavra secreta ignorando maiúsculas/
     * minúsculas e qualquer caractere que não seja letra ou número (espaços,
     * acentos gráficos ficam — só pontuação/símbolos somem). Ex.: "kai sa"
     * e "Kai'Sa" viram "KAISA" nos dois casos.
     */
    private boolean wordsMatch(String guess, String secretWord) {
        if (guess == null || secretWord == null) {
            return false;
        }
        return normalize(guess).equals(normalize(secretWord));
    }

    private String normalize(String s) {
        return s.replaceAll("[^\\p{L}\\p{N}]", "").toUpperCase();
    }

    private String capText(String text) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.length() > MAX_TEXT_LENGTH ? trimmed.substring(0, MAX_TEXT_LENGTH) : trimmed;
    }

    private void sendPrivate(String playerId, String destination, Object payload) {
        String sessionId = sessionRegistry.findSessionId(playerId);
        if (sessionId == null) {
            log.warn("Sem sessão WebSocket ativa para o jogador {}, não foi possível enviar {}", playerId, destination);
            return;
        }
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload, headerAccessor.getMessageHeaders());
    }

    private void broadcastGameState(Room room) {
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId() + "/game", GameStateMessage.of(room.getCurrentRound()));
    }

    private void broadcastRoomState(Room room) {
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), RoomStateMessage.of(room));
    }

    private static final class RotationState {
        List<String> order = List.of();
        int index = 0;
    }
}
