import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RoomState } from '../models/room.model';

@Injectable({ providedIn: 'root' })
export class RoomApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/rooms`;

  createRoom(nickname: string): Observable<RoomState> {
    return this.http.post<RoomState>(this.baseUrl, { nickname });
  }

  joinRoom(code: string, nickname: string): Observable<RoomState> {
    return this.http.post<RoomState>(`${this.baseUrl}/${code}/join`, { nickname });
  }

  getRoom(code: string): Observable<RoomState> {
    return this.http.get<RoomState>(`${this.baseUrl}/${code}`);
  }
}
