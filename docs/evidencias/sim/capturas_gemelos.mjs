// Capturas de pantalla de gemelos sintéticos abiertos en Roomix (evidencia visual).
// Uso: node capturas_gemelos.mjs <jar> <salida> G03,G20,G30   (ROOMIX_DEMO_PASSWORD = credencial de los gemelos)
import { spawn, execFileSync } from 'node:child_process';
import { mkdirSync } from 'node:fs';
import { abrir, navegador, pausa } from './comun.mjs';

const [JAR, OUT, LISTA] = process.argv.slice(2);
mkdirSync(OUT, { recursive: true });
const PANTALLAS = [['inicio', '/inicio', 'recepcion'], ['plano', '/habitaciones/plano', 'recepcion'], ['reservas', '/reservas', 'recepcion'],
  ['reportes', '/reportes', 'finanzas'], ['finanzas', '/finanzas', 'finanzas']];

for (const id of LISTA.split(',')) {
  const p = spawn('java', ['-jar', JAR], { env: { ...process.env, DATABASE_URL: `postgresql://postgres:1234@localhost:5432/roomix_sim_${id.toLowerCase()}`,
    PORT: '8080', APP_SEED_ENABLED: 'false', JWT_SECRET: 'capturas-gemelos-0123456789-abcdefghijklmnop', CORS_ALLOWED_ORIGINS: 'http://localhost:*' }, stdio: 'ignore' });
  for (let i = 0; i < 90; i++) { await pausa(1000); try { if ((await fetch('http://localhost:8080/actuator/health')).ok) break; } catch { /* arrancando */ } }
  const browser = await navegador();
  for (const [nombre, ruta, usuario] of PANTALLAS) {
    const page = await abrir(browser, usuario, ruta, { alto: 860 });
    await pausa(1500);
    await page.screenshot({ path: `${OUT}/${id}-${nombre}.png` });
    await page.close();
    console.log('ok', id, nombre);
  }
  await browser.close();
  try { execFileSync('taskkill', ['/PID', String(p.pid), '/T', '/F'], { stdio: 'ignore' }); } catch { /* terminado */ }
  await pausa(3000);
}
