import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'grash.session';

interface StoredSession {
  playerId: string;
  nickname: string;
  roomCode: string;
}

/**
 * Sessão local do jogador: sem login, sem backend — só o necessário para
 * reabrir a mesma aba (F5) sem perder o vínculo com a sala/jogador atuais.
 * Some ao fechar a aba/navegador (sessionStorage) de propósito, já que o
 * MVP não tem contas persistentes (ver ARCHITECTURE.md, decisão de auth).
 */
@Injectable({ providedIn: 'root' })
export class PlayerSessionService {
  readonly nickname = signal<string>(this.readStored()?.nickname ?? '');

  private current: StoredSession | null = this.readStored();

  setSession(playerId: string, nickname: string, roomCode: string): void {
    this.current = { playerId, nickname, roomCode };
    this.nickname.set(nickname);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(this.current));
  }

  getPlayerId(): string | null {
    return this.current?.playerId ?? null;
  }

  getRoomCode(): string | null {
    return this.current?.roomCode ?? null;
  }

  clear(): void {
    this.current = null;
    sessionStorage.removeItem(STORAGE_KEY);
  }

  private readStored(): StoredSession | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredSession;
    } catch {
      return null;
    }
  }
}
