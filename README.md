# Grash

Jogo multiplayer em tempo real — backend em Java 21 (Spring Boot) e frontend em Angular.
Veja a arquitetura completa e as decisões tomadas em [`ARCHITECTURE.md`](ARCHITECTURE.md).

Estado atual: MVP funcional — criar/entrar em sala por código, sala de espera com "pronto"
e uma arena de movimento em tempo real (WASD/setas) sincronizada via WebSocket. Sem banco de
dados (tudo em memória) e sem login (nickname temporário).

## Pré-requisitos

- **Java 21** (verificado neste ambiente: ✅ já instalado)
- **Maven 3.9+** — `mvn -v` para conferir. Se não tiver, instale com
  `winget install Apache.Maven` (ou baixe em https://maven.apache.org)
- **Node.js 20+ e npm** — `node -v` / `npm -v` para conferir. Se não tiver,
  instale com `winget install OpenJS.NodeJS.LTS` (ou baixe em https://nodejs.org)

> Neste ambiente, apenas o Java 21 estava disponível — Maven, Node e npm
> precisam ser instalados antes de rodar os passos abaixo.

## Rodando o backend

```powershell
cd "c:\Users\arthu\Documents\Projetos\Game\Grash\backend"
mvn spring-boot:run
```

Sobe em `http://localhost:8080`. Endpoints:

- `POST /api/rooms` — cria sala `{ "nickname": "..." }`
- `POST /api/rooms/{code}/join` — entra em sala `{ "nickname": "..." }`
- `GET  /api/rooms/{code}` — consulta estado da sala
- `ws://localhost:8080/ws` — endpoint STOMP/SockJS (salas + jogo em tempo real)

## Rodando o frontend

```powershell
cd "c:\Users\arthu\Documents\Projetos\Game\Grash\frontend"
npm install
npm start
```

Sobe em `http://localhost:4200`. O backend precisa estar rodando (CORS já liberado para
`http://localhost:4200` em `application.yml`).

## Testando localmente com múltiplos jogadores

Abra `http://localhost:4200` em duas abas (ou uma normal + uma anônima, já que a sessão
usa `sessionStorage` por aba): crie uma sala em uma aba, entre com o código gerado na outra,
marque "pronto" nas duas e a arena de jogo abre automaticamente para ambas.

## Publicando no GitHub

```powershell
cd "c:\Users\arthu\Documents\Projetos\Game\Grash"
git init
git add .
git commit -m "chore: MVP inicial (salas + tempo real)"
git remote add origin <URL_DO_REPOSITORIO>
git branch -M main
git push -u origin main
```
