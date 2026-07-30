# Grash

Jogo multiplayer em tempo real — backend em Java 21 (Spring Boot) e frontend em Angular.
Veja a arquitetura completa e as decisões tomadas em [`ARCHITECTURE.md`](ARCHITECTURE.md).

Estado atual: MVP funcional — criar/entrar em sala por código, sala de espera com "pronto"
e uma arena de movimento em tempo real (WASD/setas) sincronizada via WebSocket. Sem banco de
dados (tudo em memória) e sem login (nickname temporário).

## Pré-requisitos

- **Java 21** — `java -version`
- **Maven 3.9+** — `mvn -v`. Se não tiver, instale com
  `winget install Apache.Maven` (ou baixe em https://maven.apache.org)
- **Node.js 20+ e npm** — `node -v` / `npm -v`. Se não tiver,
  instale com `winget install OpenJS.NodeJS.LTS` (ou baixe em https://nodejs.org)

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

## Deploy online (Render)

O repositório já vem com tudo pronto pra publicar no [Render](https://render.com): backend
como serviço Docker, frontend como site estático, conectados via `render.yaml` na raiz.

### Passo a passo (Blueprint — recomendado)

1. Crie uma conta em https://render.com (dá pra logar direto com a conta do GitHub).
2. No dashboard, **New > Blueprint**, selecione o repositório `arthurfigon/Grash`. A Render
   lê o `render.yaml` da raiz e propõe os dois serviços automaticamente:
   - `grash-backend` (Docker, usa `backend/Dockerfile`)
   - `grash-frontend` (site estático, builda `frontend/` com `npm run build`)
3. Clique em **Apply**. A primeira build do backend demora alguns minutos (baixa
   dependências Maven do zero); o frontend é rápido.
4. Quando os dois estiverem no ar, as URLs públicas serão
   `https://grash-backend.onrender.com` e `https://grash-frontend.onrender.com`
   (exatamente os nomes usados nas envVars do `render.yaml` — é assim que os dois se
   encontram: o front já builda apontando pro back, e o back já libera CORS pro front).

> **Se o nome `grash-backend` ou `grash-frontend` já estiver em uso** (o subdomínio
> `.onrender.com` é global), a Render vai pedir outro nome. Nesse caso, edite o
> `render.yaml`: troque o nome do serviço e ajuste as `envVars` do *outro* serviço pra
> apontar pra URL nova (`GRASH_CORS_ALLOWED_ORIGINS` no backend, `API_URL`/`WS_URL` no
> frontend) antes de fazer o Apply de novo.

### Alternativa: criar os serviços manualmente

Se preferir não usar o Blueprint (ou usar outra plataforma tipo Railway/Fly.io, que também
leem Dockerfile):

- **Backend**: serviço Docker, root `backend/`, Dockerfile em `backend/Dockerfile`. Defina a
  env var `GRASH_CORS_ALLOWED_ORIGINS` com a URL pública do frontend (pode ter mais de uma,
  separadas por vírgula). `PORT` é injetada automaticamente pela plataforma.
- **Frontend**: site estático, root `frontend/`, build command `npm install && npm run build`,
  publish directory `dist/grash-frontend/browser`. Defina `API_URL` e `WS_URL` apontando pra
  URL pública do backend (ex.: `https://seu-backend.onrender.com/api` e `.../ws`) — o script
  `scripts/set-env.js` lê essas env vars no build e gera `environment.ts`. Como é SPA, configure
  um rewrite de `/*` para `/index.html` (senão recarregar `/rooms/ABC123` dá 404).

### Limitação a saber

O estado das salas ainda vive em memória (ver `ARCHITECTURE.md`) — reiniciar o serviço do
backend (deploy novo, restart, ou o plano free "dormindo" por inatividade) apaga as salas
ativas. Suficiente para mostrar o MVP; migrar pra Redis é o próximo passo se isso incomodar.
