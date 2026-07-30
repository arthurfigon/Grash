import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RoomApiService } from '../../core/services/room-api.service';
import { PlayerSessionService } from '../../core/services/player-session.service';

@Component({
  selector: 'grash-lobby',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './lobby.component.html',
  styleUrl: './lobby.component.css',
})
export class LobbyComponent implements OnInit {
  private readonly roomApi = inject(RoomApiService);
  private readonly session = inject(PlayerSessionService);
  private readonly router = inject(Router);

  readonly nickname = signal(this.session.nickname());
  readonly roomCode = signal('');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly themes = signal<string[]>([]);
  /** '' = Aleatório (sorteia um tema novo a cada rodada). */
  readonly selectedTheme = signal('');

  ngOnInit(): void {
    this.roomApi.listThemes().subscribe({
      next: (themes) => this.themes.set(themes),
      error: () => this.themes.set([]),
    });
  }

  createRoom(): void {
    if (!this.validateNickname()) {
      return;
    }
    this.loading.set(true);
    const theme = this.selectedTheme() || null;
    this.roomApi.createRoom(this.nickname().trim(), theme).subscribe({
      next: (room) => this.enterRoom(room.code, room.requestingPlayerId!),
      error: (err) => this.handleError(err),
    });
  }

  joinRoom(): void {
    if (!this.validateNickname()) {
      return;
    }
    const code = this.roomCode().trim().toUpperCase();
    if (!code) {
      this.error.set('Informe o código da sala.');
      return;
    }
    this.loading.set(true);
    this.roomApi.joinRoom(code, this.nickname().trim()).subscribe({
      next: (room) => this.enterRoom(room.code, room.requestingPlayerId!),
      error: (err) => this.handleError(err),
    });
  }

  private validateNickname(): boolean {
    this.error.set(null);
    if (this.nickname().trim().length < 2) {
      this.error.set('Nickname precisa ter pelo menos 2 caracteres.');
      return false;
    }
    return true;
  }

  private enterRoom(code: string, playerId: string): void {
    this.session.setSession(playerId, this.nickname().trim(), code);
    this.loading.set(false);
    this.router.navigate(['/rooms', code]);
  }

  private handleError(err: { error?: { message?: string } }): void {
    this.loading.set(false);
    this.error.set(err.error?.message ?? 'Não foi possível completar a operação.');
  }
}
