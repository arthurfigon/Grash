import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { RoomState } from '../models/room.model';
import { GameStateMessage, PlayerInput } from '../models/game.model';

/**
 * Uma conexão STOMP por sessão de jogo. O componente de sala/jogo chama
 * connect() ao entrar e disconnect() ao sair (ngOnDestroy) — não é mantida
 * viva entre rotas porque o MVP assume um jogador em uma sala por vez.
 */
@Injectable({ providedIn: 'root' })
export class GameSocketService {
  readonly roomState = signal<RoomState | null>(null);
  readonly gameState = signal<GameStateMessage | null>(null);
  readonly connected = signal(false);

  private client: Client | null = null;
  private roomSub: StompSubscription | null = null;
  private gameSub: StompSubscription | null = null;
  private roomId: string | null = null;

  connect(roomId: string, playerId: string): Promise<void> {
    this.roomId = roomId;

    return new Promise((resolve) => {
      const client = new Client({
        webSocketFactory: () => new SockJS(environment.wsUrl),
        reconnectDelay: 3000,
        onConnect: () => {
          this.connected.set(true);

          this.roomSub = client.subscribe(`/topic/rooms/${roomId}`, (message: IMessage) => {
            this.roomState.set(JSON.parse(message.body) as RoomState);
          });

          this.gameSub = client.subscribe(`/topic/rooms/${roomId}/game`, (message: IMessage) => {
            this.gameState.set(JSON.parse(message.body) as GameStateMessage);
          });

          client.publish({
            destination: `/app/rooms/${roomId}/join`,
            body: JSON.stringify({ playerId }),
          });

          resolve();
        },
        onDisconnect: () => this.connected.set(false),
      });

      this.client = client;
      client.activate();
    });
  }

  sendReady(playerId: string, ready: boolean): void {
    this.publish(`/app/rooms/${this.roomId}/ready`, { playerId, ready });
  }

  sendInput(input: PlayerInput): void {
    this.publish(`/app/rooms/${this.roomId}/input`, input);
  }

  disconnect(): void {
    this.roomSub?.unsubscribe();
    this.gameSub?.unsubscribe();
    this.client?.deactivate();
    this.client = null;
    this.roomId = null;
    this.roomState.set(null);
    this.gameState.set(null);
    this.connected.set(false);
  }

  private publish(destination: string, body: unknown): void {
    if (!this.client?.connected) {
      return;
    }
    this.client.publish({ destination, body: JSON.stringify(body) });
  }
}
