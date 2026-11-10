// Utilidades compartidas: sesión por rol, apertura de páginas y búsqueda de elementos.
import puppeteer from 'puppeteer-core';

export const FRONT = 'http://localhost:4200';
const API = 'http://localhost:8080/api';
const CLAVE = process.env.ROOMIX_DEMO_PASSWORD;

const sesiones = {};
export async function sesion(usuario) {
  if (!sesiones[usuario]) {
    const r = await fetch(`${API}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: usuario, password: CLAVE }),
    });
    const j = await r.json();
    sesiones[usuario] = { token: j.token, expiraEn: j.expiraEn, usuario: j.usuario };
  }
  return sesiones[usuario];
}

export const pausa = (ms) => new Promise((r) => setTimeout(r, ms));

export async function navegador() {
  return puppeteer.launch({
    executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    headless: true,
    args: ['--hide-scrollbars', '--lang=es-CO'],
  });
}

export async function abrir(browser, usuario, ruta, { ancho = 1366, alto = 860, escala = 1.5, movil = false } = {}) {
  const page = await browser.newPage();
  if (movil) await page.setViewport({ width: 390, height: alto ?? 844, isMobile: true, hasTouch: true, deviceScaleFactor: 2 });
  else await page.setViewport({ width: ancho, height: alto, deviceScaleFactor: escala });
  await page.goto(`${FRONT}/login`, { waitUntil: 'networkidle0' });
  await page.evaluate((s) => {
    localStorage.clear();
    if (s) localStorage.setItem('roomix.sesion', JSON.stringify(s));
  }, usuario ? await sesion(usuario) : null);
  await page.goto(`${FRONT}${ruta}`, { waitUntil: 'networkidle0' });
  await pausa(1300);
  return page;
}

/** Hace clic en el primer elemento visible que coincide con el selector y contiene el texto. */
export async function clic(page, selector, texto = '', espera = 900) {
  const ok = await page.evaluate(
    (sel, txt) => {
      const vis = (e) => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0; };
      const el = [...document.querySelectorAll(sel)].find((e) => vis(e) && e.textContent.replace(/\s+/g, ' ').includes(txt));
      if (el) el.click();
      return !!el;
    },
    selector,
    texto,
  );
  if (!ok) console.warn(`  (no se encontró ${selector} "${texto}")`);
  await pausa(espera);
  return ok;
}

export async function escribir(page, selector, valor) {
  const el = await page.$(selector);
  if (!el) return console.warn(`  (sin campo ${selector})`);
  await el.click({ clickCount: 3 });
  await el.type(valor, { delay: 5 });
}
