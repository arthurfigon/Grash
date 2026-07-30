// sockjs-client assume o global `global` do Node (usado ao abrir a conexão
// STOMP/SockJS na sala) — precisa existir antes de qualquer import que o carregue.
(globalThis as unknown as { global: typeof globalThis }).global = globalThis;

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));
