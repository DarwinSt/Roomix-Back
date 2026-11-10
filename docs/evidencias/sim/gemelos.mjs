// Validación con escenarios sintéticos: 30 gemelos de establecimientos reales de Neiva y Rivera (RNT, oct. 2026).
// Cada gemelo es un hotel vacío de Roomix (base de datos propia) cuya operación de 60 días se simula por la API.
// Uso: node gemelos.mjs <jar> <rnt.json> <salida> [desde] [hasta]
import { spawn, execFileSync } from 'node:child_process';
import { mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { randomBytes } from 'node:crypto';

const [JAR, RNT, OUT] = process.argv.slice(2, 5);
const DESDE = Number(process.argv[5] ?? 0);
const HASTA = Number(process.argv[6] ?? 30);
mkdirSync(OUT, { recursive: true });
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe';
const PG_DUMP = 'C:/Program Files/PostgreSQL/18/bin/pg_dump.exe';
const BASES = process.env.ROOMIX_SIM_BASES ?? `${OUT}/bases`;
const PUERTO = 8090;
const API = `http://localhost:${PUERTO}/api`;
const CLAVE = process.env.ROOMIX_SIM_CLAVE ?? randomBytes(12).toString('base64url') + 'Aa1*';   // credencial de prueba, no se imprime
const SEMILLA = 20261102;
const HORIZONTE = 60;

// ------------------------------------------------------------------ azar reproducible
function mulberry32(a) {
  return () => { a |= 0; a = (a + 0x6D2B79F5) | 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
}
let azar = mulberry32(SEMILLA);
const unif = (a, b) => a + (b - a) * azar();
const ent = (a, b) => Math.floor(unif(a, b + 1));
const elegir = (pares) => { let r = azar() * pares.reduce((s, p) => s + p[1], 0); for (const [v, w] of pares) { if ((r -= w) <= 0) return v; } return pares.at(-1)[0]; };
const poisson = (l) => { let L = Math.exp(-l), k = 0, p = 1; do { k++; p *= azar(); } while (p > L); return k - 1; };

// ------------------------------------------------------------------ selección estratificada de 30 gemelos
const ESTRATOS = [['1 a 4', 1, 4, 3], ['5 a 10', 5, 10, 10], ['11 a 20', 11, 20, 10], ['21 a 50', 21, 50, 6], ['Más de 50', 51, 1e6, 1]];
const rnt = JSON.parse(readFileSync(RNT, 'utf8')).filter((x) => ['NEIVA', 'RIVERA'].includes(x.municipio))
  .map((x, i) => ({ idx: i, municipio: x.municipio, tipo: x.sub_categoria, habitaciones: Math.round(Number(x.habitaciones)), camas: Math.round(Number(x.camas ?? 0)) }));
const gemelos = [];
for (const [estrato, a, b, n] of ESTRATOS) {
  const grupo = rnt.filter((x) => x.habitaciones >= a && x.habitaciones <= b);
  const copia = [...grupo];
  for (let k = 0; k < n; k++) gemelos.push({ ...copia.splice(Math.floor(azar() * copia.length), 1)[0], estrato });
}
gemelos.forEach((g, i) => { g.id = `G${String(i + 1).padStart(2, '0')}`; });
writeFileSync(`${OUT}/gemelos.json`, JSON.stringify(gemelos, null, 1));

// ------------------------------------------------------------------ parámetros (fuente o supuesto declarado)
const PARAM = {
  ocupacionBase: 0.492,            // DANE, EMA agosto 2026 (La República, 2026)
  factorOcupacion: [0.75, 1.25],   // supuesto: dispersión entre establecimientos
  adrPorTamano: [[10, 110000], [20, 140000], [50, 180000], [1e6, 230000]], // supuesto (COP por noche)
  multTipo: { INDIVIDUAL: 0.8, DOBLE: 1, TRIPLE: 1.25, SUITE: 1.8 },
  estancia: [[1, 40], [2, 30], [3, 18], [4, 8], [5, 2], [6, 1], [7, 1]],   // supuesto
  anticipacion: [['mismo', 45], ['corta', 35], ['larga', 20]],              // supuesto: 0 / 1–7 / 8–30 días
  cancelacion: 0.08,               // supuesto (la encuesta reporta cancelaciones no registradas)
  canales: [['WHATSAPP', 33], ['TELEFONO', 33], ['PRESENCIAL', 28], ['SITIO_WEB', 6]], // encuesta (julio 2026)
  extranjeros: 0.06,               // supuesto
  servicios: 0.2,                  // supuesto
  pagoParcial: 0.15,               // supuesto: abona al reservar y paga el saldo al salir; el resto paga todo al reservar
};

// ------------------------------------------------------------------ cliente HTTP con medición de latencia
let lat = [];
const tokens = {};
async function llamar(etq, rol, metodo, ruta, cuerpo) {
  const t0 = performance.now();
  const r = await fetch(API + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tokens[rol]}` },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
  const txt = await r.text();
  lat.push([etq, performance.now() - t0, r.status]);
  let datos = null;
  try { datos = JSON.parse(txt); } catch { datos = txt; }
  return { s: r.status, d: datos };
}
async function login(u) {
  const r = await fetch(`${API}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: CLAVE }) });
  tokens[u] = (await r.json()).token;
}

// ------------------------------------------------------------------ servidor por gemelo
function psql(sql) { execFileSync(PSQL, ['-U', 'postgres', '-h', 'localhost', '-q', '-c', sql], { env: { ...process.env, PGPASSWORD: '1234' }, stdio: 'pipe' }); }
async function puertoLibre() {
  try { await fetch(`http://localhost:${PUERTO}/actuator/health`); return false; } catch { return true; }
}
function detener(p) {
  try { execFileSync('taskkill', ['/PID', String(p.pid), '/T', '/F'], { stdio: 'ignore' }); } catch { /* ya terminó */ }
}
async function arrancar(db) {
  if (!(await puertoLibre())) throw new Error(`el puerto ${PUERTO} está ocupado por otro proceso`);
  psql(`DROP DATABASE IF EXISTS ${db} WITH (FORCE)`);
  psql(`CREATE DATABASE ${db}`);
  const p = spawn('java', ['-Xmx768m', '-jar', JAR], {
    env: { ...process.env, DATABASE_URL: `postgresql://postgres:1234@localhost:5432/${db}`, PORT: String(PUERTO), APP_SEED_ENABLED: 'false',
      ADMIN_PASSWORD: CLAVE, DEMO_USERS_PASSWORD: CLAVE, JWT_SECRET: randomBytes(32).toString('hex'), CORS_ALLOWED_ORIGINS: 'http://localhost:*' },
    stdio: 'ignore',
  });
  for (let i = 0; i < 120; i++) {
    await new Promise((r) => setTimeout(r, 1000));
    try { const r = await fetch(`http://localhost:${PUERTO}/actuator/health`); if (r.ok) return p; } catch { /* arrancando */ }
  }
  detener(p);
  throw new Error('el backend no arrancó');
}

const fecha = (d) => d.toISOString().slice(0, 10);
const HOY = new Date(new Date().toISOString().slice(0, 10) + 'T00:00:00Z');
const dia = (n) => new Date(HOY.getTime() + n * 864e5);   // n relativo a hoy (negativo = pasado)

// ------------------------------------------------------------------ simulación de un gemelo
async function simular(g) {
  azar = mulberry32(SEMILLA + g.idx);
  lat = [];
  const N = g.habitaciones;
  const errores = [];
  const ocupObjetivo = Math.min(0.85, Math.max(0.2, PARAM.ocupacionBase * unif(...PARAM.factorOcupacion)));
  const adrBase = PARAM.adrPorTamano.find(([max]) => N <= max)[1] * unif(0.85, 1.15);
  for (const u of ['admin', 'recepcion', 'housekeeping', 'finanzas']) await login(u);

  // Configuración del hotel
  const habs = [];
  for (let k = 0; k < N; k++) {
    const piso = 1 + Math.floor(k / 20);
    const tipo = N <= 4 ? elegir([['DOBLE', 60], ['TRIPLE', 40]]) : elegir([['INDIVIDUAL', 25], ['DOBLE', 50], ['TRIPLE', 17], ['SUITE', 8]]);
    const precio = Math.round(adrBase * PARAM.multTipo[tipo] / 1000) * 1000;
    const r = await llamar('Crear habitación', 'admin', 'POST', '/habitaciones', { numero: `${piso}${String((k % 20) + 1).padStart(2, '0')}`, tipoHabitacion: tipo,
      descripcion: `Gemelo ${g.id}`, precioNoche: precio, piso, capacidad: { INDIVIDUAL: 1, DOBLE: 2, TRIPLE: 3, SUITE: 4 }[tipo], caracteristicas: ['WiFi'] });
    if (r.s >= 300) throw new Error(`habitación ${r.s} ${JSON.stringify(r.d).slice(0, 150)}`);
    habs.push({ id: r.d.id, precio, tipo, ocupada: [] });
  }
  const personal = (await llamar('Crear personal', 'admin', 'POST', '/personal', { nombre: 'Camarera de pisos (gemelo)', rol: 'Camarera de pisos', departamento: 'Housekeeping', activo: true })).d.id;
  const servicio = (await llamar('Crear servicio', 'admin', 'POST', '/servicios/catalogo', { nombre: 'Lavandería', categoria: 'LAVANDERIA', precio: 30000, unidad: 'por bolsa' })).d.id;

  // Demanda: llegadas diarias ~ Poisson(ocupación × N / estancia media)
  const estanciaMedia = PARAM.estancia.reduce((s, [v, w]) => s + v * w, 0) / 100;
  const solicitudes = [];
  for (let a = -HORIZONTE; a <= 15; a++) {
    const n = poisson(ocupObjetivo * N / estanciaMedia);
    for (let k = 0; k < n; k++) {
      const noches = elegir(PARAM.estancia);
      const tipoAnt = elegir(PARAM.anticipacion);
      const ant = tipoAnt === 'mismo' ? 0 : tipoAnt === 'corta' ? ent(1, 7) : ent(8, 30);
      const b = Math.max(a - ant, -HORIZONTE);
      if (b > -1) continue;                        // solo reservas hechas dentro del horizonte (pasado)
      const cancela = azar() < PARAM.cancelacion && a > b;
      solicitudes.push({ a, noches, b, cancela, diaCancel: cancela ? ent(b, a - 1) : null, canal: elegir(PARAM.canales),
        extranjero: azar() < PARAM.extranjeros, servicio: azar() < PARAM.servicios, parcial: azar() < PARAM.pagoParcial });
    }
  }
  const reservas = [];       // libro de la simulación (verdad de referencia)
  let rechazadas = 0, h01 = 0, huesped = 0, demandaPerdida = 0;
  const libre = (h, a, n) => !h.ocupada.some(([x, y]) => a < y && x < a + n);

  for (let t = -HORIZONTE; t <= -1; t++) {
    // 1. Salidas: check-out, pago y limpieza
    for (const r of reservas.filter((x) => x.estado === 'EN_CURSO' && x.a + x.noches === t)) {
      const co = await llamar('Check-out', 'recepcion', 'PATCH', `/reservas/${r.id}/check-out`);
      if (co.s >= 300) { errores.push(`check-out ${co.s} ${JSON.stringify(co.d).slice(0, 120)}`); continue; }
      r.estado = 'FINALIZADA';
      const f = co.d.factura;
      if (f) {
        r.factura = f.total;
        // La factura ya trae aplicado lo abonado al reservar; en el check-out se paga el saldo (saldo de abono parcial y servicios)
        const saldo = Number(f.saldo ?? Number(f.total) - Number(f.totalPagado ?? 0));
        if (saldo > 0) {
          const pg = await llamar('Registrar pago', 'recepcion', 'POST', `/facturas/${f.id}/pagos`, { valor: saldo, metodo: elegir([['EFECTIVO', 50], ['TARJETA_DEBITO', 25], ['TRANSFERENCIA', 25]]) });
          if (pg.s >= 300 || pg.d.estado !== 'PAGADA') errores.push(`pago ${pg.s} ${pg.d?.estado}`);
        } else if (f.estado !== 'PAGADA') errores.push(`factura sin saldo en estado ${f.estado}`);
      } else errores.push('check-out sin factura');
      const act = await llamar('Incidencias activas', 'housekeeping', 'GET', `/incidencias/habitacion/${r.hab.id}/activas`);
      for (const inc of act.d ?? []) {
        await llamar('Limpieza: asignar', 'housekeeping', 'PATCH', `/incidencias/${inc.id}/asignar`, { personalId: personal });
        const ini = await llamar('Limpieza: iniciar', 'housekeeping', 'PATCH', `/incidencias/${inc.id}/iniciar`);
        for (const ta of ini.d.tareas ?? []) if (!ta.completada) await llamar('Limpieza: tarea', 'housekeeping', 'PATCH', `/incidencias/${inc.id}/tareas/${ta.id}`, { completada: true });
        const fin = await llamar('Limpieza: finalizar', 'housekeeping', 'PATCH', `/incidencias/${inc.id}/finalizar`);
        if (fin.s >= 300) errores.push(`limpieza ${fin.s} ${JSON.stringify(fin.d).slice(0, 120)}`);
      }
    }
    // 2. Cancelaciones del día
    for (const r of reservas.filter((x) => x.estado === 'CONFIRMADA' && x.diaCancel === t)) {
      const c = await llamar('Cancelar reserva', 'recepcion', 'PATCH', `/reservas/${r.id}/cancelar`, { motivo: 'El huésped canceló (simulación)' });
      if (c.s >= 300) errores.push(`cancelar ${c.s}`);
      else {
        r.estado = 'CANCELADA'; r.hab.ocupada = r.hab.ocupada.filter((o) => o !== r.tramo);
        r.penalidad = Number(c.d.valorPenalidad ?? 0); r.devuelto = Number(c.d.valorDevuelto ?? 0);
      }
    }
    // 3. Reservas hechas hoy
    for (const s of solicitudes.filter((x) => x.b === t)) {
      const candidatas = habs.filter((h) => libre(h, s.a, s.noches));
      if (!candidatas.length) { demandaPerdida++; continue; }
      const h = candidatas[Math.floor(azar() * candidatas.length)];
      const ocupadaHoy = h.ocupada.some(([x, y]) => x <= t && t < y) || reservas.some((r) => r.hab === h && r.estado === 'CONFIRMADA' && r.a < s.a);
      const doc = `${g.idx}${String(reservas.length + 1).padStart(5, '0')}${ent(10, 99)}`;
      const hu = await llamar('Registrar huésped', 'recepcion', 'POST', '/huespedes', {
        nombre: 'Huésped', apellidos: `Sintético ${g.id}-${reservas.length + 1}`, tipoDocumento: s.extranjero ? 'PASAPORTE' : 'CEDULA', numeroDocumento: doc,
        email: `h${doc}@gemelo.test`, telefono: '+57 300 000 0000', nacionalidad: s.extranjero ? elegir([['Estados Unidos', 3], ['España', 2], ['Ecuador', 2], ['Venezuela', 2]]) : 'Colombia',
        autorizaTratamientoDatos: true });
      if (hu.s >= 300) { errores.push(`huésped ${hu.s} ${JSON.stringify(hu.d).slice(0, 120)}`); continue; }
      // Toda reserva se crea con su pago (RES-015): completo, o abono parcial (el mínimo de la política, o la mitad si es 0 %)
      const cz = await llamar('Cotizar reserva', 'recepcion', 'GET', `/reservas/cotizacion?habitacionId=${h.id}&fechaEntrada=${fecha(dia(s.a))}&fechaSalida=${fecha(dia(s.a + s.noches))}&canal=${s.canal}&huespedId=${hu.d.id}`);
      if (cz.s >= 300) { errores.push(`cotización ${cz.s} ${JSON.stringify(cz.d).slice(0, 120)}`); continue; }
      const totalReserva = Number(cz.d.totalConImpuesto);
      const abono = s.parcial ? Math.max(Number(cz.d.abonoMinimo), Math.round(totalReserva / 2)) : totalReserva;
      const re = await llamar('Crear reserva', 'recepcion', 'POST', '/reservas', { habitacionId: h.id, huespedId: hu.d.id, fechaEntrada: fecha(dia(s.a)), fechaSalida: fecha(dia(s.a + s.noches)), canal: s.canal,
        abonoInicial: { valor: abono, metodo: elegir([['EFECTIVO', 40], ['TRANSFERENCIA', 45], ['TARJETA_DEBITO', 15]]) } });
      if (re.s >= 300) { rechazadas++; if (re.d?.codigo === 'RES-011') huesped++; errores.push(`reserva ${re.s} ${re.d?.codigo} ${re.d?.detail ?? ''}`.slice(0, 160)); continue; }
      if (ocupadaHoy) h01++;
      const tramo = [s.a, s.a + s.noches];
      h.ocupada.push(tramo);
      reservas.push({ ...s, id: re.d.id, hab: h, tramo, tarifa: Number(re.d.tarifaNoche), estado: 'CONFIRMADA', abono });
    }
    // 4. Llegadas
    for (const r of reservas.filter((x) => x.estado === 'CONFIRMADA' && x.a === t)) {
      const ci = await llamar('Check-in', 'recepcion', 'PATCH', `/reservas/${r.id}/check-in`);
      if (ci.s >= 300) { errores.push(`check-in ${ci.s} ${JSON.stringify(ci.d?.detail ?? ci.d).slice(0, 160)}`); continue; }
      r.estado = 'EN_CURSO';
      if (r.servicio) {
        const so = await llamar('Solicitar servicio', 'recepcion', 'POST', '/servicios/solicitudes', { reservaId: r.id, servicioId: servicio, cantidad: 1 });
        if (so.s < 300) {
          await llamar('Servicio: asignar', 'recepcion', 'PATCH', `/servicios/solicitudes/${so.d.id}/asignar`, { responsableId: personal });
          await llamar('Servicio: estado', 'recepcion', 'PATCH', `/servicios/solicitudes/${so.d.id}/estado`, { estado: 'EN_PROCESO' });
          await llamar('Servicio: estado', 'recepcion', 'PATCH', `/servicios/solicitudes/${so.d.id}/estado`, { estado: 'COMPLETADA' });
          r.servicios = 30000;
        } else errores.push(`servicio ${so.s}`);
      }
    }
  }

  // ---------------------------------------------------------------- verificación contra la verdad de la simulación
  const desde = -HORIZONTE, hasta = -1;
  let noches = 0, ingreso = 0;
  for (const r of reservas.filter((x) => x.estado !== 'CANCELADA')) {
    for (let d = Math.max(r.a, desde); d < Math.min(r.a + r.noches, hasta + 1); d++) { noches++; ingreso += r.tarifa; }
  }
  const disp = N * HORIZONTE;
  const rep = (await llamar('Reporte de ocupación', 'finanzas', 'GET', `/reportes/ocupacion?desde=${fecha(dia(desde))}&hasta=${fecha(dia(hasta))}`)).d;
  const todas = (await llamar('Listar reservas', 'recepcion', 'GET', '/reservas')).d;
  let cruces = 0;
  const porHab = {};
  for (const r of todas.filter((x) => x.estadoReserva !== 'CANCELADA')) (porHab[r.habitacionId] ??= []).push(r);
  for (const lista of Object.values(porHab)) {
    lista.sort((x, y) => x.fechaEntrada.localeCompare(y.fechaEntrada));
    for (let i = 1; i < lista.length; i++) if (lista[i].fechaEntrada < lista[i - 1].fechaSalida) cruces++;
  }
  const facturas = [];
  for (let pg = 0; ; pg++) {
    const fact = (await llamar('Listar facturas', 'finanzas', 'GET', `/facturas?pagina=${pg}&tamano=100`)).d;
    const lote = fact.contenido ?? fact;
    facturas.push(...lote);
    if (!lote.length || (fact.totalPaginas !== undefined && pg + 1 >= fact.totalPaginas)) break;
  }
  const totFact = facturas.reduce((s, f) => s + Number(f.total), 0);
  const totPag = facturas.reduce((s, f) => s + Number(f.totalPagado), 0);
  const res = (await llamar('Resumen financiero', 'finanzas', 'GET', `/finanzas/resumen?desde=${fecha(dia(-1))}&hasta=${fecha(dia(1))}`)).d;
  const ingresosFin = Number(res.totalIngresos ?? res.ingresos ?? 0);
  // Caja: los abonos entran como ingreso al reservar. Los de reservas finalizadas ya figuran como pagos de su factura;
  // los de reservas aún sin facturar (confirmadas, en curso o canceladas) son el dinero recibido por adelantado.
  const abonosSinFacturar = reservas.filter((r) => r.estado !== 'FINALIZADA').reduce((s, r) => s + (r.abono ?? 0), 0);
  // Cancelaciones: la devolución sale como egreso y la penalidad se retiene
  const canceladas = reservas.filter((r) => r.estado === 'CANCELADA');
  const abonosCancelados = canceladas.reduce((s, r) => s + (r.abono ?? 0), 0);
  const penalidadRetenida = canceladas.reduce((s, r) => s + (r.penalidad ?? 0), 0);
  const devueltoCancelaciones = canceladas.reduce((s, r) => s + (r.devuelto ?? 0), 0);

  // Rendimiento con los datos ya cargados (20 repeticiones por consulta)
  for (let i = 0; i < 20; i++) {
    await llamar('Consulta: dashboard', 'recepcion', 'GET', '/dashboard');
    await llamar('Consulta: habitaciones', 'recepcion', 'GET', '/habitaciones');
    await llamar('Consulta: disponibilidad', 'recepcion', 'GET', `/reservas/disponibilidad?fechaEntrada=${fecha(dia(1))}&fechaSalida=${fecha(dia(3))}`);
    await llamar('Consulta: reporte 60 días', 'finanzas', 'GET', `/reportes/ocupacion?desde=${fecha(dia(desde))}&hasta=${fecha(dia(hasta))}`);
  }
  return {
    gemelo: g, parametros: { ocupacionObjetivo: ocupObjetivo, adrBase },
    simulacion: { solicitudes: solicitudes.length, reservas: reservas.length, canceladas: reservas.filter((r) => r.estado === 'CANCELADA').length,
      demandaPerdida, nochesVendidas: noches, nochesDisponibles: disp, ocupacion: noches / disp, adr: noches ? ingreso / noches : 0, revpar: ingreso / disp, ingreso },
    roomix: { nochesVendidas: rep.nochesVendidas, nochesDisponibles: rep.nochesDisponibles, ocupacionPct: rep.ocupacionPct, adr: Number(rep.adr), revpar: Number(rep.revpar), ingreso: Number(rep.ingresoAlojamiento) },
    integridad: { cruces, rechazadas, rechazosHuesped: huesped, reservasAnticipadasEnHabitacionNoLibre: h01, facturas: facturas.length, totalFacturado: totFact, totalPagado: totPag, ingresosFinanzas: ingresosFin, abonosSinFacturar, abonosCancelados, penalidadRetenida, devueltoCancelaciones, errores: errores.slice(0, 30), nErrores: errores.length },
    llamadas: lat.length, latencias: lat,
  };
}

// ------------------------------------------------------------------ ejecución
for (const g of gemelos.slice(DESDE, HASTA)) {
  if (existsSync(`${OUT}/${g.id}.json`)) { console.log('ya', g.id); continue; }
  // ROOMIX_SIM_PREFIJO permite una corrida de prueba sin tocar las bases de evidencia roomix_sim_gNN
  const db = `${process.env.ROOMIX_SIM_PREFIJO ?? "roomix_sim_"}${g.id.toLowerCase()}`;
  const t0 = Date.now();
  const proc = await arrancar(db);
  try {
    const r = await simular(g);
    writeFileSync(`${OUT}/${g.id}.json`, JSON.stringify(r));
    console.log(g.id, g.municipio, g.habitaciones, 'hab', r.simulacion.reservas, 'reservas', r.llamadas, 'llamadas',
      'ocup', (r.simulacion.ocupacion * 100).toFixed(1), 'vs', r.roomix.ocupacionPct, 'cruces', r.integridad.cruces, 'errores', r.integridad.nErrores,
      Math.round((Date.now() - t0) / 1000) + 's');
  } catch (e) {
    console.log('ERROR', g.id, e.message);
  } finally {
    detener(proc);
    for (let i = 0; i < 30 && !(await puertoLibre()); i++) await new Promise((r) => setTimeout(r, 1000));
    // Evidencia: la base del gemelo se conserva y además se exporta a un archivo restaurable (pg_restore)
    mkdirSync(BASES, { recursive: true });
    try {
      execFileSync(PG_DUMP, ['-U', 'postgres', '-h', 'localhost', '-Fc', '-f', `${BASES}/${db}.dump`, db], { env: { ...process.env, PGPASSWORD: '1234' }, stdio: 'pipe' });
    } catch (e) { console.log('pg_dump', g.id, e.message); }
  }
}
