export type PlayerStatus = 'WAITING' | 'READY' | 'PLAYING';

export interface PlayerView {
  id: string;
  nickname: string;
  status: PlayerStatus;
}
