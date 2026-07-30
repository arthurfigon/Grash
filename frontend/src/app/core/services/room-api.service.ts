import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RoomState } from '../models/room.model';

@Injectable({ providedIn: 'root' })
export class RoomApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/rooms`;

  /** @param theme null/vazio = sorteia um tema novo a cada rodada; senão, tema fixo pro jogo inteiro. */
  createRoom(nickname: string, theme: string | null): Observable<RoomState> {
    return this.http.post<RoomState>(this.baseUrl, { nickname, theme });
  }

  joinRoom(code: string, nickname: string): Observable<RoomState> {
    return this.http.post<RoomState>(`${this.baseUrl}/${code}/join`, { nickname });
  }

  getRoom(code: string): Observable<RoomState> {
    return this.http.get<RoomState>(`${this.baseUrl}/${code}`);
  }

  listThemes(): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}/themes`);
  }
}
