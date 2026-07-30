export interface PlayerPosition {
  playerId: string;
  nickname: string;
  x: number;
  y: number;
}

export interface GameStateMessage {
  tick: number;
  players: PlayerPosition[];
}

export interface PlayerInput {
  playerId: string;
  up: boolean;
  down: boolean;
  left: boolean;
  right: boolean;
}
