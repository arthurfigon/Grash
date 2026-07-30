import { Component, computed, effect, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RoomApiService } from '../../core/services/room-api.service';
import { GameSocketService } from '../../core/services/game-socket.service';
import { PlayerSessionService } from '../../core/services/player-session.service';

@Component({
  selector: 'grash-room',
  standalone: true,
  imports: [],
  templateUrl: './room.component.html',
  styleUrl: './room.component.css',
})
export class RoomComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roomApi = inject(RoomApiService);
  private readonly session = inject(PlayerSessionService);
  readonly gameSocket = inject(GameSocketService);

  readonly code = this.route.snapshot.paramMap.get('code')!;
  readonly playerId = this.session.getPlayerId();

  readonly room = this.gameSocket.roomState;
  readonly myPlayer = computed(() => this.room()?.players.find((p) => p.id === this.playerId) ?? null);
  readonly isReady = computed(() => this.myPlayer()?.status === 'READY');
  readonly canStart = computed(() => (this.room()?.players.length ?? 0) >= 2);

  constructor() {
    effect(() => {
      if (this.room()?.status === 'IN_PROGRESS') {
        this.router.navigate(['/rooms', this.code, 'play']);
      }
    });
  }

  ngOnInit(): void {
    if (!this.playerId) {
      this.router.navigate(['/']);
      return;
    }

    this.roomApi.getRoom(this.code).subscribe({
      next: (room) => {
        this.gameSocket.disconnect();
        this.gameSocket.connect(room.roomId, this.playerId!);
      },
      error: () => this.router.navigate(['/']),
    });
  }

  ngOnDestroy(): void {
    if (this.room()?.status !== 'IN_PROGRESS') {
      this.gameSocket.disconnect();
    }
  }

  toggleReady(): void {
    if (!this.playerId) {
      return;
    }
    this.gameSocket.sendReady(this.playerId, !this.isReady());
  }

  copyCode(): void {
    navigator.clipboard?.writeText(this.code);
  }

  disconnect(): void {
    this.gameSocket.disconnect();
    this.session.clear();
    this.router.navigate(['/']);
  }
}
