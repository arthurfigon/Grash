import { PlayerView } from './player.model';

export type RoomStatus = 'WAITING' | 'IN_PROGRESS' | 'FINISHED';

export interface RoomState {
  roomId: string;
  code: string;
  status: RoomStatus;
  ownerId: string;
  /** Tema fixo escolhido na criação da sala; null = sorteia um tema novo a cada rodada. */
  fixedTheme: string | null;
  players: PlayerView[];
  requestingPlayerId: string | null;
}
