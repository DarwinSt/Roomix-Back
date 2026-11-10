// Servidor E2E aislado: sirve el build del frontend con fallback SPA (como Render con la regla /* → /index.html)
// y reenvía /api al backend E2E. Uso: node servidor.mjs <carpeta-browser> <puerto> <puerto-backend>
import { createServer, request } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';

const [RAIZ, PUERTO, BACK] = process.argv.slice(2);
const TIPOS = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json', '.png': 'image/png',
  '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.woff2': 'font/woff2', '.gif': 'image/gif', '.webp': 'image/webp' };

createServer(async (req, res) => {
  if (req.url.startsWith('/api/')) {
    const p = request({ host: 'localhost', port: BACK, path: req.url, method: req.method, headers: req.headers }, (r) => {
      res.writeHead(r.statusCode, r.headers); r.pipe(res);
    });
    p.on('error', () => { res.writeHead(502); res.end(); });
    req.pipe(p);
    return;
  }
  const ruta = normalize(decodeURIComponent(req.url.split('?')[0])).replace(/^([/\\])+/, '');
  let archivo = join(RAIZ, ruta);
  try { if (!(await stat(archivo)).isFile()) throw 0; } catch { archivo = join(RAIZ, 'index.html'); }
  res.writeHead(200, { 'Content-Type': TIPOS[extname(archivo)] ?? 'application/octet-stream' });
  res.end(await readFile(archivo));
}).listen(Number(PUERTO), () => console.log(`E2E front en http://localhost:${PUERTO} → backend ${BACK}`));
