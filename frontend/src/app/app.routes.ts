import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/lobby/lobby.component').then((m) => m.LobbyComponent),
  },
  {
    path: 'rooms/:code',
    loadComponent: () => import('./features/room/room.component').then((m) => m.RoomComponent),
  },
  {
    path: 'rooms/:code/play',
    loadComponent: () => import('./features/game/game.component').then((m) => m.GameComponent),
  },
  { path: '**', redirectTo: '' },
];
