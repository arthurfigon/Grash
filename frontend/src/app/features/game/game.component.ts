import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { GameSocketService } from '../../core/services/game-socket.service';
import { PlayerSessionService } from '../../core/services/player-session.service';

@Component({
  selector: 'grash-game',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './game.component.html',
  styleUrl: './game.component.css',
})
export class GameComponent implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly session = inject(PlayerSessionService);
  readonly gameSocket = inject(GameSocketService);

  readonly playerId = this.session.getPlayerId();
  readonly clueText = signal('');
  readonly guessText = signal('');

  readonly room = this.gameSocket.roomState;
  readonly game = this.gameSocket.gameState;
  readonly myCard = this.gameSocket.myCard;

  readonly players = computed(() => this.room()?.players ?? []);
  readonly gameOver = computed(() => this.room()?.status === 'FINISHED');
  readonly isMyTurn = computed(() => !!this.playerId && this.game()?.currentTurnPlayerId === this.playerId);
  readonly isImpostor = computed(() => this.myCard()?.impostor ?? false);
  readonly hasVoted = computed(() => this.game()?.votedPlayerIds.includes(this.playerId ?? '') ?? false);
  readonly hasGuessed = computed(() => this.game()?.impostorHasGuessed ?? false);
  readonly voteCandidates = computed(() => this.players().filter((p) => p.id !== this.playerId));
  readonly rankedPlayers = computed(() => [...this.players()].sort((a, b) => b.score - a.score));
  readonly topScore = computed(() => this.rankedPlayers()[0]?.score ?? 0);

  constructor() {
    const code = this.route.snapshot.paramMap.get('code')!;
    if (!this.playerId || !this.gameSocket.connected()) {
      this.router.navigate(['/rooms', code]);
    }
  }

  ngOnDestroy(): void {
    this.gameSocket.disconnect();
  }

  nicknameOf(id: string | null): string {
    if (!id) {
      return '';
    }
    return this.players().find((p) => p.id === id)?.nickname ?? '???';
  }

  cluesFor(playerId: string): string[] {
    return (this.game()?.clues ?? []).filter((c) => c.playerId === playerId).map((c) => c.text);
  }

  submitClue(): void {
    const text = this.clueText().trim();
    if (!text || !this.playerId) {
      return;
    }
    this.gameSocket.sendClue(this.playerId, text);
    this.clueText.set('');
  }

  vote(votedForId: string): void {
    if (!this.playerId || this.hasVoted()) {
      return;
    }
    this.gameSocket.sendVote(this.playerId, votedForId);
  }

  submitGuess(): void {
    const text = this.guessText().trim();
    if (!text || !this.playerId || this.hasGuessed()) {
      return;
    }
    this.gameSocket.sendGuess(this.playerId, text);
    this.guessText.set('');
  }

  leaveToLobby(): void {
    this.gameSocket.disconnect();
    this.session.clear();
    this.router.navigate(['/']);
  }
}
