export type PlayerStatus = 'WAITING' | 'READY';

export interface PlayerView {
  id: string;
  nickname: string;
  status: PlayerStatus;
  score: number;
}
