import { PlayerView } from './player.model';

export type RoomStatus = 'WAITING' | 'IN_PROGRESS' | 'FINISHED';

export interface RoomState {
  roomId: string;
  code: string;
  status: RoomStatus;
  ownerId: string;
  players: PlayerView[];
  requestingPlayerId: string | null;
}
