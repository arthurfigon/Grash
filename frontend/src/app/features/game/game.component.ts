import { Component, ElementRef, HostListener, OnDestroy, effect, inject, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GameSocketService } from '../../core/services/game-socket.service';
import { PlayerSessionService } from '../../core/services/player-session.service';
import { GAME_CONSTANTS } from '../../core/models/game-constants';

const PLAYER_COLORS = ['#5b5bf0', '#3ddc84', '#ff6b6b', '#ffb84d', '#4dd2ff', '#c76bff', '#ff6bd2', '#a3d977'];

@Component({
  selector: 'grash-game',
  standalone: true,
  imports: [],
  templateUrl: './game.component.html',
  styleUrl: './game.component.css',
})
export class GameComponent implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly session = inject(PlayerSessionService);
  private readonly gameSocket = inject(GameSocketService);

  readonly canvasRef = viewChild<ElementRef<HTMLCanvasElement>>('canvas');
  readonly arenaWidth = GAME_CONSTANTS.ARENA_WIDTH;
  readonly arenaHeight = GAME_CONSTANTS.ARENA_HEIGHT;

  private readonly playerId = this.session.getPlayerId();
  private readonly pressed = { up: false, down: false, left: false, right: false };

  constructor() {
    const code = this.route.snapshot.paramMap.get('code')!;
    if (!this.playerId || !this.gameSocket.connected()) {
      this.router.navigate(['/rooms', code]);
      return;
    }

    effect(() => {
      const canvas = this.canvasRef()?.nativeElement;
      const state = this.gameSocket.gameState();
      if (!canvas || !state) {
        return;
      }
      this.draw(canvas, state.players);
    });
  }

  ngOnDestroy(): void {
    this.gameSocket.disconnect();
  }

  disconnect(): void {
    this.gameSocket.disconnect();
    this.session.clear();
    this.router.navigate(['/']);
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    this.updateKey(event.key, true);
  }

  @HostListener('window:keyup', ['$event'])
  onKeyUp(event: KeyboardEvent): void {
    this.updateKey(event.key, false);
  }

  private updateKey(key: string, isDown: boolean): void {
    const map: Record<string, keyof typeof this.pressed> = {
      ArrowUp: 'up',
      w: 'up',
      W: 'up',
      ArrowDown: 'down',
      s: 'down',
      S: 'down',
      ArrowLeft: 'left',
      a: 'left',
      A: 'left',
      ArrowRight: 'right',
      d: 'right',
      D: 'right',
    };
    const direction = map[key];
    if (!direction || this.pressed[direction] === isDown) {
      return;
    }
    this.pressed[direction] = isDown;
    this.gameSocket.sendInput({ playerId: this.playerId!, ...this.pressed });
  }

  private draw(canvas: HTMLCanvasElement, players: { playerId: string; nickname: string; x: number; y: number }[]): void {
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return;
    }

    ctx.fillStyle = '#0d0e16';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    players.forEach((player, index) => {
      const color = PLAYER_COLORS[index % PLAYER_COLORS.length];
      ctx.beginPath();
      ctx.arc(player.x, player.y, GAME_CONSTANTS.PLAYER_RADIUS, 0, Math.PI * 2);
      ctx.fillStyle = color;
      ctx.fill();

      if (player.playerId === this.playerId) {
        ctx.lineWidth = 3;
        ctx.strokeStyle = '#ffffff';
        ctx.stroke();
      }

      ctx.fillStyle = '#e8e8f0';
      ctx.font = '12px system-ui';
      ctx.textAlign = 'center';
      ctx.fillText(player.nickname, player.x, player.y - GAME_CONSTANTS.PLAYER_RADIUS - 6);
    });
  }
}
