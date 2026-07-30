package com.grash.server.dto;

import com.grash.server.domain.GameConstants;
import com.grash.server.domain.RoundPhase;
import com.grash.server.domain.RoundState;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Estado público da rodada — broadcastado pra sala inteira. Nunca contém a
 * palavra secreta nem a identidade do impostor antes da fase REVEAL (isso
 * vai só na carta privada de cada jogador, ver {@link PrivateCardMessage}).
 */
public record GameStateMessage(
        int round,
        int totalRounds,
        String phase,
        String theme,
        List<String> turnOrder,
        String currentTurnPlayerId,
        int clueLap,
        List<ClueEntryDto> clues,
        List<String> votedPlayerIds,
        /** true assim que o impostor manda o palpite dele — antes da revelação, não diz qual foi o palpite. */
        boolean impostorHasGuessed,
        // Campos abaixo só vêm preenchidos na fase REVEAL:
        String impostorId,
        String secretWord,
        String impostorGuess,
        boolean impostorGuessedCorrectly,
        Map<String, Long> voteTally,
        Map<String, Integer> scoreDeltas
) {

    public static GameStateMessage of(RoundState round) {
        boolean revealed = round.getPhase() == RoundPhase.REVEAL;

        List<ClueEntryDto> clues = round.getClues().stream().map(ClueEntryDto::from).toList();
        List<String> votedPlayerIds = List.copyOf(round.getVotes().keySet());
        String currentTurnPlayerId = round.getPhase() == RoundPhase.CLUE_GIVING ? round.currentTurnPlayerId() : null;

        Map<String, Long> voteTally = revealed
                ? round.getVotes().values().stream().collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                : Map.of();

        return new GameStateMessage(
                round.getRoundNumber(),
                GameConstants.TOTAL_ROUNDS,
                round.getPhase().name(),
                round.getTheme(),
                round.getTurnOrder(),
                currentTurnPlayerId,
                round.getClueLap(),
                clues,
                votedPlayerIds,
                round.hasImpostorGuessed(),
                revealed ? round.getImpostorId() : null,
                revealed ? round.getSecretWord() : null,
                revealed ? round.getImpostorGuess() : null,
                revealed && round.isImpostorGuessedCorrectly(),
                voteTally,
                revealed ? round.getScoreDeltas() : Map.of()
        );
    }
}
