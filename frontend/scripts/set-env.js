// Gera src/environments/environment.ts a partir de variáveis de ambiente de
// build (API_URL / WS_URL). Roda antes de "ng build" — o build de produção
// importa environment.ts diretamente (sem fileReplacement), então é esse
// arquivo que precisa apontar para o backend real em cada deploy.
// Local (sem as env vars setadas) cai nos defaults de localhost.
const fs = require('fs');
const path = require('path');

const apiUrl = process.env.API_URL || 'http://localhost:8080/api';
const wsUrl = process.env.WS_URL || 'http://localhost:8080/ws';

const content = `// Gerado por scripts/set-env.js a partir de API_URL / WS_URL — não editar à mão.
export const environment = {
  production: true,
  apiUrl: '${apiUrl}',
  wsUrl: '${wsUrl}',
};
`;

const target = path.join(__dirname, '..', 'src', 'environments', 'environment.ts');
fs.writeFileSync(target, content);
console.log(`[set-env] environment.ts gerado com apiUrl=${apiUrl} wsUrl=${wsUrl}`);
