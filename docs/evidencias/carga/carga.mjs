// Prueba de carga de Roomix con la estructura de un plan de JMeter:
// grupos de hilos (usuarios virtuales) con rampa de subida y duración, login "una sola vez" por hilo,
// temporizador aleatorio uniforme entre muestras, aserciones de código HTTP y "Aggregate Report" por etiqueta.
// Uso: node carga.mjs <url-base> <salida> ; la credencial de prueba se recibe en la variable ROOMIX_CARGA_CLAVE.
import { writeFileSync, mkdirSync } from 'node:fs';

const BASE = process.argv[2];
const OUT = process.argv[3];
mkdirSync(OUT, { recursive: true });
const CLAVE = process.env.ROOMIX_CARGA_CLAVE;

const GRUPOS = [
  { nombre: 'Carga normal', hilos: 10, rampa: 10, duracion: 120 },
  { nombre: 'Carga alta', hilos: 50, rampa: 30, duracion: 120 },
  { nombre: 'Estrés', hilos: 100, rampa: 30, duracion: 120 },
];
const PAUSA = [500, 1500];   // Uniform Random Timer (ms)
const espera = (ms) => new Promise((r) => setTimeout(r, ms));
const fecha = (d) => d.toISOString().slice(0, 10);

async function muestra(reg, etq, metodo, ruta, token, cuerpo, aceptados = [200, 201]) {
  const t0 = performance.now();
  let status = 0, bytes = 0;
  try {
    const r = await fetch(BASE + ruta, {
      method: metodo,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    });
    const txt = await r.text();
    status = r.status; bytes = txt.length;
    reg.push({ etq, t: Date.now(), ms: performance.now() - t0, ok: aceptados.includes(status), status, bytes });
    try { return { status, d: JSON.parse(txt) }; } catch { return { status, d: txt }; }
  } catch (e) {
    reg.push({ etq, t: Date.now(), ms: performance.now() - t0, ok: false, status: 0, bytes });
    return { status: 0, d: null };
  }
}

async function hilo(reg, n, fin, habitaciones) {
  // Once Only Controller: login de recepción y de finanzas
  const rec = (await muestra(reg, '00 Login', 'POST', '/api/auth/login', null, { username: 'recepcion', password: CLAVE })).d?.token;
  const fin2 = (await muestra(reg, '00 Login', 'POST', '/api/auth/login', null, { username: 'finanzas', password: CLAVE })).d?.token;
  let it = 0;
  const pausa = () => espera(PAUSA[0] + Math.random() * (PAUSA[1] - PAUSA[0]));
  while (Date.now() < fin) {
    it++;
    await muestra(reg, '01 Panel del día', 'GET', '/api/dashboard', rec); await pausa();
    await muestra(reg, '02 Listar habitaciones', 'GET', '/api/habitaciones', rec); await pausa();
    const ent = new Date(Date.now() + (400 + n * 7 + (it % 7)) * 864e5);   // fechas propias de cada hilo: sin cruces entre hilos
    await muestra(reg, '03 Consultar disponibilidad', 'GET', `/api/reservas/disponibilidad?fechaEntrada=${fecha(ent)}&fechaSalida=${fecha(new Date(ent.getTime() + 864e5))}`, rec); await pausa();
    const doc = `7${String(n).padStart(3, '0')}${String(it).padStart(5, '0')}${Math.floor(Math.random() * 90 + 10)}`;
    const hu = await muestra(reg, '04 Registrar huésped', 'POST', '/api/huespedes', rec, {
      nombre: 'Carga', apellidos: `Usuario ${n}-${it}`, tipoDocumento: 'CEDULA', numeroDocumento: doc, email: `c${doc}@carga.test`,
      telefono: '+57 300 000 0000', nacionalidad: 'Colombia', autorizaTratamientoDatos: true }, [201]);
    await pausa();
    if (hu.status === 201) {
      const hab = habitaciones[(n * 13 + it) % habitaciones.length];
      const re = await muestra(reg, '05 Crear reserva', 'POST', '/api/reservas', rec, {
        habitacionId: hab, huespedId: hu.d.id, fechaEntrada: fecha(ent), fechaSalida: fecha(new Date(ent.getTime() + 864e5)), canal: 'WHATSAPP',
        // Toda reserva lleva su pago (RES-015); el gemelo G30 tiene la política de abono mínimo en 0 %
        abonoInicial: { valor: 50000, metodo: 'EFECTIVO' } }, [201]);
      await pausa();
      if (re.status === 201) {
        await muestra(reg, '06 Cancelar reserva', 'PATCH', `/api/reservas/${re.d.id}/cancelar`, rec, { motivo: 'Prueba de carga' }); await pausa();
      }
    }
    await muestra(reg, '07 Listar reservas del día', 'GET', `/api/reservas?fechaDesde=${fecha(new Date())}&fechaHasta=${fecha(new Date())}`, rec); await pausa();
    if (it % 5 === 0) { await muestra(reg, '08 Reporte de ocupación (30 días)', 'GET', '/api/reportes/ocupacion', fin2); await pausa(); }
  }
}

function percentil(arr, p) { if (!arr.length) return 0; const s = [...arr].sort((a, b) => a - b); return s[Math.min(s.length - 1, Math.ceil(p / 100 * s.length) - 1)]; }
function agregado(reg, duracion) {
  const porEtq = {};
  for (const r of reg) (porEtq[r.etq] ??= []).push(r);
  const filas = Object.keys(porEtq).sort().map((k) => resumen(k, porEtq[k], duracion));
  filas.push(resumen('TOTAL', reg, duracion));
  return filas;
}
function resumen(etq, rs, duracion) {
  const ms = rs.map((r) => r.ms);
  return { etiqueta: etq, muestras: rs.length, promedio: Math.round(ms.reduce((s, x) => s + x, 0) / rs.length), mediana: Math.round(percentil(ms, 50)),
    p90: Math.round(percentil(ms, 90)), p95: Math.round(percentil(ms, 95)), p99: Math.round(percentil(ms, 99)),
    min: Math.round(Math.min(...ms)), max: Math.round(Math.max(...ms)), errorPct: +(100 * rs.filter((r) => !r.ok).length / rs.length).toFixed(2),
    throughput: +(rs.length / duracion).toFixed(2), kbps: +(rs.reduce((s, r) => s + r.bytes, 0) / 1024 / duracion).toFixed(1) };
}

const resultados = [];
const tokAdmin = (await (await fetch(`${BASE}/api/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: 'admin', password: CLAVE }) })).json()).token;
const listaH = await (await fetch(`${BASE}/api/habitaciones`, { headers: { Authorization: `Bearer ${tokAdmin}` } })).json();
const habitaciones = (Array.isArray(listaH) ? listaH : listaH.contenido).map((h) => h.id);
for (const g of GRUPOS) {
  const reg = [];
  const inicio = Date.now();
  const fin = inicio + (g.rampa + g.duracion) * 1000;
  const hilos = [];
  for (let n = 0; n < g.hilos; n++) {
    hilos.push(espera((g.rampa * 1000 * n) / g.hilos).then(() => hilo(reg, n + GRUPOS.indexOf(g) * 1000, fin, habitaciones)));
  }
  await Promise.all(hilos);
  const dur = (Date.now() - inicio) / 1000;
  // Serie de tiempo cada 5 s (para el gráfico "Response Times Over Time")
  const serie = [];
  for (let t = 0; t < dur; t += 5) {
    const v = reg.filter((r) => r.t - inicio >= t * 1000 && r.t - inicio < (t + 5) * 1000);
    serie.push({ t, muestras: v.length, promedio: v.length ? Math.round(v.reduce((s, r) => s + r.ms, 0) / v.length) : 0, errores: v.filter((r) => !r.ok).length });
  }
  const errores = {};
  for (const r of reg.filter((x) => !x.ok)) errores[`${r.etq} → HTTP ${r.status}`] = (errores[`${r.etq} → HTTP ${r.status}`] ?? 0) + 1;
  resultados.push({ grupo: g, duracionReal: dur, agregado: agregado(reg, dur), serie, errores });
  console.log(g.nombre, JSON.stringify(resultados.at(-1).agregado.at(-1)));
  await espera(10000);   // enfriamiento entre grupos
}
writeFileSync(`${OUT}/carga-resultados.json`, JSON.stringify({ fecha: new Date().toISOString(), base: BASE, grupos: resultados }, null, 1));
