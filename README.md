# Grash

O jogo do Impostor — backend em Java 21 (Spring Boot) e frontend em Angular, tudo em tempo
real via WebSocket. Veja a arquitetura completa e as decisões de design em
[`ARCHITECTURE.md`](ARCHITECTURE.md).

Como funciona: crie uma sala (código de 6 caracteres, mínimo 3 jogadores) escolhendo um tema
(ex.: League of Legends, Profissões, Animais...) ou "Aleatório" — se escolher um tema, ele vale
pras 10 rodadas; se for "Aleatório", cada rodada sorteia um tema novo. Todos marcam "pronto" e
o jogo sorteia a palavra da rodada: todos exceto o Impostor a recebem; o Impostor só sabe o
tema. Em turnos, cada jogador dá 3 dicas sobre a palavra sem dizê-la — depois todos exceto o
Impostor votam anonimamente em quem acha que é o Impostor, enquanto o Impostor tenta adivinhar
a palavra secreta. Se o Impostor acerta, ganha um bônus e ninguém mais pontua na rodada; se
erra, a pontuação normal da votação vale (voto certo pontua quem votou, voto errado pontua o
Impostor). 10 rodadas ao todo, vence quem tiver mais pontos.

Sem banco de dados, sem contas de usuário — tudo em memória, nickname temporário por sessão.

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

Sobe em `http://localhost:8080` (carrega o banco de palavras de `word-bank.json` na subida —
sem banco de dados, sem configuração extra). Endpoints:

- `POST /api/rooms` — cria sala `{ "nickname": "...", "theme": "..." }` (`theme` opcional; nulo/vazio
  ou tema desconhecido = sorteia um tema novo a cada rodada)
- `POST /api/rooms/{code}/join` — entra em sala `{ "nickname": "..." }`
- `GET  /api/rooms/{code}` — consulta estado da sala
- `GET  /api/themes` — lista os temas disponíveis no banco de palavras
- `ws://localhost:8080/ws` — endpoint STOMP/SockJS (sala, dicas, votos, palpite do Impostor, estado da rodada)

## Rodando o frontend

```powershell
cd "c:\Users\arthu\Documents\Projetos\Game\Grash\frontend"
npm install
npm start
```

Sobe em `http://localhost:4200`. O backend precisa estar rodando (CORS já liberado para
`http://localhost:4200` em `application.yml`).

## Testando localmente com múltiplos jogadores

Abra `http://localhost:4200` em 3+ abas (a sessão usa `sessionStorage` por aba, então cada
uma é um jogador independente): crie uma sala numa aba, entre com o código nas outras, marque
"pronto" em todas — o jogo começa automaticamente quando o mínimo de 3 jogadores estiver pronto.

## Deploy online (Render)

O repositório já vem com tudo pronto pra publicar no [Render](https://render.com): backend
como serviço Docker, frontend como site estático — sem banco de dados, `render.yaml` na raiz
conecta os dois.

### Passo a passo (Blueprint — recomendado)

1. Crie uma conta em https://render.com (dá pra logar direto com a conta do GitHub).
2. No dashboard, **New > Blueprint**, selecione o repositório `arthurfigon/Grash`. A Render
   lê o `render.yaml` da raiz e propõe os dois serviços automaticamente:
   - `grash-backend` (Docker, usa `backend/Dockerfile`)
   - `grash-frontend` (site estático, builda `frontend/` com `npm run build`)
3. Clique em **Apply**. O backend demora alguns minutos na primeira build (baixa dependências
   Maven do zero); o frontend é rápido.
4. Quando os dois estiverem no ar, as URLs públicas serão
   `https://grash-backend.onrender.com` e `https://grash-frontend.onrender.com`
   (exatamente os nomes usados nas envVars do `render.yaml` — é assim que os dois se
   encontram: o front já builda apontando pro back, e o back já libera CORS pro front).

> **Se o nome `grash-backend`/`grash-frontend` já estiver em uso** (o subdomínio
> `.onrender.com` é global), a Render vai pedir outro nome. Nesse caso, edite o
> `render.yaml`: troque o nome do serviço e ajuste as `envVars` do *outro* serviço pra
> apontar pra URL nova (`GRASH_CORS_ALLOWED_ORIGINS` no backend, `API_URL`/`WS_URL` no
> frontend) antes de fazer o Apply de novo.

### Alternativa: criar os serviços manualmente

Se preferir não usar o Blueprint (ou usar outra plataforma tipo Railway/Fly.io, que também
leem Dockerfile):

- **Backend**: serviço Docker, root `backend/`, Dockerfile em `backend/Dockerfile`. Defina
  `GRASH_CORS_ALLOWED_ORIGINS` com a URL pública do frontend (pode ter mais de uma, separadas
  por vírgula). `PORT` é injetada automaticamente pela plataforma.
- **Frontend**: site estático, root `frontend/`, build command `npm install && npm run build`,
  publish directory `dist/grash-frontend/browser`. Defina `API_URL` e `WS_URL` apontando pra
  URL pública do backend (ex.: `https://seu-backend.onrender.com/api` e `.../ws`) — o script
  `scripts/set-env.js` lê essas env vars no build e gera `environment.ts`. Como é SPA, configure
  um rewrite de `/*` para `/index.html` (senão recarregar `/rooms/ABC123` dá 404).

### Limitação a saber

Tudo (salas, jogadores, rodadas) vive em memória — reiniciar o backend (deploy novo, restart,
ou o plano free "dormindo" por inatividade) apaga as salas ativas. Ver `ARCHITECTURE.md`.
