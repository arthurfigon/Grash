export type RoundPhase = 'CLUE_GIVING' | 'VOTING' | 'REVEAL';

/** A carta privada de um jogador — nunca vista pelos outros. */
export interface PrivateCard {
  theme: string;
  impostor: boolean;
  word: string | null;
}

export interface ClueEntry {
  playerId: string;
  lap: number;
  text: string;
}

/** Estado público da rodada — igual para todo mundo na sala. */
export interface GameState {
  round: number;
  totalRounds: number;
  phase: RoundPhase;
  theme: string;
  turnOrder: string[];
  currentTurnPlayerId: string | null;
  clueLap: number;
  clues: ClueEntry[];
  votedPlayerIds: string[];
  /** true assim que o impostor manda o palpite — mesmo antes da revelação. */
  impostorHasGuessed: boolean;
  // só preenchidos quando phase === 'REVEAL':
  impostorId: string | null;
  secretWord: string | null;
  impostorGuess: string | null;
  impostorGuessedCorrectly: boolean;
  voteTally: Record<string, number>;
  scoreDeltas: Record<string, number>;
}
