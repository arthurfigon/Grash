import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { RoomState } from '../models/room.model';
import { GameState, PrivateCard } from '../models/game.model';

/**
 * Uma conexão STOMP por sessão de jogo. O componente de sala/jogo chama
 * connect() ao entrar e disconnect() ao sair (ngOnDestroy) — não é mantida
 * viva entre rotas porque o MVP assume um jogador em uma sala por vez.
 *
 * A carta privada (/user/queue/card) chega só pra essa sessão — o servidor
 * endereça pelo sessionId do STOMP, sem precisar de login (ver
 * GameService.sendPrivate no backend).
 */
@Injectable({ providedIn: 'root' })
export class GameSocketService {
  readonly roomState = signal<RoomState | null>(null);
  readonly gameState = signal<GameState | null>(null);
  readonly myCard = signal<PrivateCard | null>(null);
  readonly connected = signal(false);

  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];
  private roomId: string | null = null;

  connect(roomId: string, playerId: string): Promise<void> {
    this.roomId = roomId;

    return new Promise((resolve) => {
      const client = new Client({
        webSocketFactory: () => new SockJS(environment.wsUrl),
        reconnectDelay: 3000,
        onConnect: () => {
          this.connected.set(true);

          this.subscriptions.push(
            client.subscribe(`/topic/rooms/${roomId}`, (message: IMessage) => {
              this.roomState.set(JSON.parse(message.body) as RoomState);
            }),
            client.subscribe(`/topic/rooms/${roomId}/game`, (message: IMessage) => {
              this.gameState.set(JSON.parse(message.body) as GameState);
            }),
            client.subscribe('/user/queue/card', (message: IMessage) => {
              this.myCard.set(JSON.parse(message.body) as PrivateCard);
            })
          );

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

  sendClue(playerId: string, text: string): void {
    this.publish(`/app/rooms/${this.roomId}/clue`, { playerId, text });
  }

  sendVote(playerId: string, votedForId: string): void {
    this.publish(`/app/rooms/${this.roomId}/vote`, { playerId, votedForId });
  }

  sendGuess(playerId: string, guess: string): void {
    this.publish(`/app/rooms/${this.roomId}/guess`, { playerId, guess });
  }

  disconnect(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions = [];
    this.client?.deactivate();
    this.client = null;
    this.roomId = null;
    this.roomState.set(null);
    this.gameState.set(null);
    this.myCard.set(null);
    this.connected.set(false);
  }

  private publish(destination: string, body: unknown): void {
    if (!this.client?.connected) {
      return;
    }
    this.client.publish({ destination, body: JSON.stringify(body) });
  }
}
