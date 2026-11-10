// E2E de los flujos nuevos (pago al reservar, abonos, promociones, políticas, cancelación con penalidad, facturación con
// abonos, gastos por pagar y recurrentes, reportes) y barrido de pantallas por rol (F5, consola y red).
// Uso: node nuevos.mjs <carpeta-evidencias>
import { mkdirSync, writeFileSync } from 'node:fs';
import { abrir, navegador, pausa, sesion, FRONT } from './comun.mjs';

const OUT = process.argv[2];
mkdirSync(OUT, { recursive: true });
const API = 'http://localhost:8080/api';
const resultados = [];
const sufijo = Date.now().toString().slice(-6);
const fecha = (d) => d.toISOString().slice(0, 10);
const dia = (n) => fecha(new Date(Date.now() + n * 864e5));
const igual = (a, b) => Math.abs(Number(a) - Number(b)) < 0.01;

async function api(usuario, metodo, ruta, cuerpo) {
  const t = (await sesion(usuario)).token;
  const r = await fetch(API + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
  const texto = await r.text();
  let datos = null;
  try { datos = JSON.parse(texto); } catch { datos = texto; }
  return { status: r.status, datos };
}
function afirmar(cond, msg) { if (!cond) throw new Error(msg); }
async function caso(id, flujo, fn) {
  try {
    const obs = await fn();
    resultados.push({ id, flujo, resultado: 'OK', observacion: obs ?? '' });
    console.log('OK  ', id, obs ?? '');
  } catch (e) {
    resultados.push({ id, flujo, resultado: 'Falla', observacion: e.message });
    console.log('FALLA', id, e.message);
  }
}
const lista = (d) => d?.contenido ?? d?.content ?? d ?? [];

// ---------------------------------------------------------------- datos base
const habs = lista((await api('admin', 'GET', '/habitaciones?size=500')).datos);
let nHab = 0;
async function habitacionNueva(tipo = 'DOBLE', precio = 200000) {
  const r = await api('admin', 'POST', '/habitaciones', { numero: `8${sufijo.slice(-2)}${nHab++}`, tipoHabitacion: tipo, descripcion: 'E2E', precioNoche: precio });
  afirmar(r.status === 201, `crear habitación ${r.status} ${JSON.stringify(r.datos).slice(0, 150)}`);
  return r.datos;
}
let nHu = 0;
async function huespedNuevo(nacionalidad = 'Colombia') {
  const doc = `9${sufijo}${nHu++}`;
  const r = await api('recepcion', 'POST', '/huespedes', { nombre: 'E2E', apellidos: `Prueba ${nHu}`, tipoDocumento: nacionalidad === 'Colombia' ? 'CEDULA' : 'PASAPORTE',
    numeroDocumento: doc, email: `e2e${doc}@correo.test`, telefono: '3000000000', nacionalidad, activo: true, autorizaTratamientoDatos: true });
  afirmar(r.status === 201, `crear huésped ${r.status} ${JSON.stringify(r.datos).slice(0, 150)}`);
  return r.datos;
}
const cotizar = (h, e, s, extra = '') => api('recepcion', 'GET', `/reservas/cotizacion?habitacionId=${h}&fechaEntrada=${e}&fechaSalida=${s}${extra}`);

// ---------------------------------------------------------------- políticas
await caso('N-01', 'Políticas de reserva', async () => {
  const rec = await api('recepcion', 'PUT', '/politicas-reserva', { abonoMinimoPct: 90, penalidadCancelacionPct: 90 });
  afirmar(rec.status === 403, `recepción pudo cambiar la política (${rec.status})`);
  const malo = await api('admin', 'PUT', '/politicas-reserva', { abonoMinimoPct: 150, penalidadCancelacionPct: 10 });
  afirmar(malo.status === 400, `política 150 % aceptada (${malo.status})`);
  const ok = await api('admin', 'PUT', '/politicas-reserva', { abonoMinimoPct: 50, penalidadCancelacionPct: 20 });
  afirmar(ok.status === 200 && igual(ok.datos.abonoMinimoPct, 50), `actualizar política ${ok.status}`);
  const lee = await api('recepcion', 'GET', '/politicas-reserva');
  afirmar(lee.status === 200 && igual(lee.datos.penalidadCancelacionPct, 20), `recepción no lee la política ${lee.status}`);
  return 'Solo Admin la cambia (recepción 403, 150 % → 400); queda abono mínimo 50 % y penalidad 20 %';
});

// ---------------------------------------------------------------- promociones
let promo, habPromo;
await caso('N-02', 'Promociones', async () => {
  const p = await api('admin', 'POST', '/promociones', { nombre: `Estadía larga E2E ${sufijo}`, minimoNoches: 5, tipoDescuento: 'VALOR_FIJO', valor: 30000, activa: true });
  afirmar(p.status === 201 || p.status === 200, `crear promoción ${p.status} ${JSON.stringify(p.datos).slice(0, 200)}`);
  promo = p.datos;
  const rec = await api('recepcion', 'POST', '/promociones', { nombre: 'x', tipoDescuento: 'VALOR_FIJO', valor: 1 });
  afirmar(rec.status === 403, `recepción pudo crear promociones (${rec.status})`);
  habPromo = await habitacionNueva('DOBLE', 200000);
  const cinco = lista((await api('recepcion', 'GET', `/promociones/aplicables?habitacionId=${habPromo.id}&fechaEntrada=${dia(30)}&fechaSalida=${dia(35)}`)).datos);
  const dos = lista((await api('recepcion', 'GET', `/promociones/aplicables?habitacionId=${habPromo.id}&fechaEntrada=${dia(30)}&fechaSalida=${dia(32)}`)).datos);
  afirmar(cinco.some((x) => x.id === promo.id), 'la promoción no aparece para 5 noches');
  afirmar(!dos.some((x) => x.id === promo.id), 'la promoción aparece para 2 noches');
  const c = (await cotizar(habPromo.id, dia(30), dia(35), `&promocionId=${promo.id}`)).datos;
  afirmar(igual(c.subtotal, 1000000) && igual(c.descuento, 30000) && igual(c.total, 970000), `cotización ${JSON.stringify(c)}`);
  afirmar(igual(c.impuesto, 970000 * 0.19) && igual(c.totalConImpuesto, 970000 * 1.19), `IVA sobre el subtotal con descuento ${c.impuesto}`);
  afirmar(igual(c.abonoMinimo, Math.round(970000 * 1.19 * 0.5)), `abono mínimo ${c.abonoMinimo}`);
  const no = await cotizar(habPromo.id, dia(30), dia(32), `&promocionId=${promo.id}`);
  afirmar(no.status === 422, `promoción aplicada a 2 noches (${no.status})`);
  return `5 noches × $200.000 − $30.000 = $970.000 + IVA = ${c.totalConImpuesto}; abono mínimo ${c.abonoMinimo}; no aplica a 2 noches (422)`;
});

// ---------------------------------------------------------------- reserva con pago obligatorio
let reservaPromo, cotPromo, huPromo, abonosPromo = [];
await caso('N-03', 'Reserva con pago obligatorio', async () => {
  huPromo = await huespedNuevo();
  cotPromo = (await cotizar(habPromo.id, dia(0), dia(5), `&promocionId=${promo.id}&huespedId=${huPromo.id}`)).datos;
  const base = { habitacionId: habPromo.id, huespedId: huPromo.id, fechaEntrada: dia(0), fechaSalida: dia(5), canal: 'WHATSAPP', promocionId: promo.id };
  const sin = await api('recepcion', 'POST', '/reservas', base);
  afirmar(sin.status === 422 && sin.datos.codigo === 'RES-015', `sin pago ${sin.status} ${sin.datos.codigo}`);
  const poco = await api('recepcion', 'POST', '/reservas', { ...base, abonoInicial: { valor: cotPromo.abonoMinimo - 1, metodo: 'EFECTIVO' } });
  afirmar(poco.status === 422 && poco.datos.codigo === 'RES-015', `abono bajo el mínimo ${poco.status}`);
  const mucho = await api('recepcion', 'POST', '/reservas', { ...base, abonoInicial: { valor: Number(cotPromo.totalConImpuesto) + 1, metodo: 'EFECTIVO' } });
  afirmar(mucho.status === 422, `abono mayor que el total aceptado (${mucho.status})`);
  const estado = await api('recepcion', 'PATCH', `/habitaciones/${habPromo.id}/estado`, { estado: 'RESERVADO', fechaEntrada: dia(0), fechaSalida: dia(5), huespedId: huPromo.id });
  afirmar(estado.status === 422 && estado.datos.codigo === 'RES-016', `reserva por cambio de estado ${estado.status}`);
  const huerfanos = lista((await api('recepcion', 'GET', `/huespedes/${huPromo.id}/reservas`)).datos);
  afirmar(huerfanos.length === 0, `los intentos rechazados dejaron ${huerfanos.length} reserva(s)`);
  const ok = await api('recepcion', 'POST', '/reservas', { ...base, abonoInicial: { valor: cotPromo.abonoMinimo, metodo: 'TRANSFERENCIA', referencia: 'TRX-1' } });
  afirmar(ok.status === 201, `reserva con abono ${ok.status} ${JSON.stringify(ok.datos).slice(0, 200)}`);
  reservaPromo = ok.datos;
  afirmar(igual(reservaPromo.descuento, 30000) && reservaPromo.promocionNombre, `reserva sin promoción ${reservaPromo.descuento}`);
  const h = (await api('recepcion', 'GET', `/habitaciones/${habPromo.id}`)).datos;
  afirmar(h.estado === 'RESERVADO', `habitación quedó ${h.estado}`);
  return `Sin pago, bajo el mínimo, sobre el total y por cambio de estado: rechazados sin dejar reservas; con abono de ${cotPromo.abonoMinimo}: confirmada y habitación Reservada`;
});

await caso('N-04', 'Abonos de la reserva', async () => {
  const r = (await api('recepcion', 'GET', `/reservas/${reservaPromo.id}/abonos`)).datos;
  afirmar(igual(r.totalReserva, cotPromo.totalConImpuesto), `total de abonos ${r.totalReserva} ≠ cotización ${cotPromo.totalConImpuesto}`);
  afirmar(igual(r.totalAbonado, cotPromo.abonoMinimo) && igual(r.saldo, r.totalReserva - r.totalAbonado), `abonado/saldo ${r.totalAbonado}/${r.saldo}`);
  const det = (await api('recepcion', 'GET', `/reservas/${reservaPromo.id}`)).datos;
  const totalDetalle = Number(det.totalEstimado);
  const ex = await api('recepcion', 'POST', `/reservas/${reservaPromo.id}/abonos`, { valor: Number(r.saldo) + 1, metodo: 'EFECTIVO' });
  afirmar(ex.status === 422, `abono mayor que el saldo aceptado ${ex.status}`);
  const seg = await api('recepcion', 'POST', `/reservas/${reservaPromo.id}/abonos`, { valor: 100000, metodo: 'EFECTIVO' });
  afirmar(seg.status < 300, `segundo abono ${seg.status}`);
  const r2 = (await api('recepcion', 'GET', `/reservas/${reservaPromo.id}/abonos`)).datos;
  abonosPromo = r2.abonos.map((a) => a.id);
  afirmar(r2.abonos.length === 2 && igual(r2.totalAbonado, Number(cotPromo.abonoMinimo) + 100000), `abonos ${r2.abonos.length} ${r2.totalAbonado}`);
  const fin = lista((await api('finanzas', 'GET', `/finanzas/movimientos?texto=${encodeURIComponent('Abono')}&tamano=200`)).datos)
    .filter((m) => m.tipo === 'INGRESO' && m.abonoId && r2.abonos.some((a) => a.id === m.abonoId));
  afirmar(fin.length === 2, `ingresos de abonos en Finanzas: ${fin.length}`);
  return `Total ${r.totalReserva} (detalle muestra totalEstimado ${totalDetalle}); 2 abonos visibles; saldo ${r2.saldo}; abono mayor al saldo 422; cada abono es un ingreso en Finanzas`;
});

// ---------------------------------------------------------------- estadía, factura y caja
let facturaPromo;
await caso('N-05', 'Check-in → check-out → factura con abonos', async () => {
  const ci = await api('recepcion', 'PATCH', `/reservas/${reservaPromo.id}/check-in`);
  afirmar(ci.status === 200, `check-in ${ci.status} ${JSON.stringify(ci.datos).slice(0, 150)}`);
  const co = await api('recepcion', 'PATCH', `/reservas/${reservaPromo.id}/check-out`, {});
  afirmar(co.status === 200 && co.datos.factura, `check-out ${co.status}`);
  facturaPromo = co.datos.factura;
  // El check-out el mismo día cobra las noches reservadas (5): la factura debe coincidir con lo cotizado
  afirmar(igual(facturaPromo.total, cotPromo.totalConImpuesto), `factura ${facturaPromo.total} ≠ cotización ${cotPromo.totalConImpuesto}`);
  const abonado = Number(cotPromo.abonoMinimo) + 100000;
  afirmar(igual(facturaPromo.totalPagado, abonado) && facturaPromo.estado === 'PAGADA_PARCIAL', `abonos en factura ${facturaPromo.totalPagado} ${facturaPromo.estado}`);
  const pg = await api('recepcion', 'POST', `/facturas/${facturaPromo.id}/pagos`, { valor: Number(facturaPromo.saldo), metodo: 'TARJETA_DEBITO' });
  afirmar(pg.status < 300 && pg.datos.estado === 'PAGADA', `pago del saldo ${pg.status} ${pg.datos.estado}`);
  const movs = lista((await api('finanzas', 'GET', `/finanzas/movimientos?tamano=500`)).datos);
  const ingresosReserva = movs.filter((m) => m.tipo === 'INGRESO' && (m.facturaId === facturaPromo.id || abonosPromo.includes(m.abonoId)));
  const suma = ingresosReserva.reduce((s, m) => s + Number(m.valor), 0);
  afirmar(igual(suma, facturaPromo.total), `ingresos en Finanzas por la reserva ${suma} ≠ factura ${facturaPromo.total} (doble conteo o faltante)`);
  const l = (pg.datos.lineas ?? pg.datos.items ?? []).map((x) => x.descripcion).join(' | ');
  afirmar(/Estadía larga E2E/.test(l), `la factura no menciona la promoción: ${l}`);
  return `Factura ${facturaPromo.numero} = cotización (${facturaPromo.total}); abonos aplicados ${facturaPromo.totalPagado}; saldo pagado; Finanzas suma exactamente el total (sin doble conteo)`;
});

await caso('N-06', 'Anular factura pagada con abonos', async () => {
  const an = await api('finanzas', 'PATCH', `/facturas/${facturaPromo.id}/anular`, { motivo: 'Prueba E2E de anulación con abonos' });
  afirmar(an.status === 200 && an.datos.estado === 'ANULADA', `anular ${an.status}`);
  afirmar(an.datos.pagos.every((p) => p.devuelto), 'quedaron pagos sin devolver');
  const movs = lista((await api('finanzas', 'GET', `/finanzas/movimientos?tipo=EGRESO&tamano=500`)).datos);
  const dev = movs.filter((m) => (m.concepto + ' ' + (m.referencia ?? '')).includes(facturaPromo.numero)).reduce((s, m) => s + Number(m.valor), 0);
  afirmar(igual(dev, facturaPromo.total), `egresos de devolución ${dev} ≠ total ${facturaPromo.total}`);
  const abonoMov = lista((await api('finanzas', 'GET', `/finanzas/movimientos?tamano=500`)).datos).find((m) => m.abonoId);
  const noAnula = await api('finanzas', 'PATCH', `/finanzas/movimientos/${abonoMov.id}/anular`, { motivo: 'x' });
  afirmar(noAnula.status >= 400, `se pudo anular a mano el ingreso de un abono (${noAnula.status})`);
  return `Todos los pagos (incluidos los abonos aplicados) quedan devueltos; egresos de devolución = ${dev}; el ingreso de un abono no se anula a mano (${noAnula.status})`;
});

// ---------------------------------------------------------------- cancelación con penalidad
await caso('N-07', 'Cancelación con penalidad', async () => {
  const h = await habitacionNueva('INDIVIDUAL', 150000);
  const hu = await huespedNuevo();
  const c = (await cotizar(h.id, dia(40), dia(42), `&huespedId=${hu.id}`)).datos;
  const r = await api('recepcion', 'POST', '/reservas', { habitacionId: h.id, huespedId: hu.id, fechaEntrada: dia(40), fechaSalida: dia(42), canal: 'TELEFONO',
    abonoInicial: { valor: c.totalConImpuesto, metodo: 'EFECTIVO' } });
  afirmar(r.status === 201, `reserva ${r.status}`);
  const can = await api('recepcion', 'PATCH', `/reservas/${r.datos.id}/cancelar`, { motivo: 'El huésped cancela (E2E)' });
  afirmar(can.status === 200 && can.datos.estadoReserva === 'CANCELADA', `cancelar ${can.status}`);
  const pen = Number(c.totalConImpuesto) * 0.2;
  afirmar(igual(can.datos.valorPenalidad, pen) && igual(can.datos.valorDevuelto, Number(c.totalConImpuesto) - pen), `penalidad ${can.datos.valorPenalidad} / devuelto ${can.datos.valorDevuelto}`);
  const ab = (await api('recepcion', 'GET', `/reservas/${r.datos.id}/abonos`)).datos;
  afirmar(igual(ab.valorPenalidad, pen) && ab.abonos.every((a) => a.devuelto), `resumen de abonos tras cancelar ${JSON.stringify(ab).slice(0, 200)}`);
  const hh = (await api('recepcion', 'GET', `/habitaciones/${h.id}`)).datos;
  afirmar(hh.estado === 'LIBRE', `habitación tras cancelar: ${hh.estado}`);
  const otra = await api('recepcion', 'PATCH', `/reservas/${r.datos.id}/cancelar`, { motivo: 'otra vez' });
  afirmar(otra.status === 422, `cancelar dos veces ${otra.status}`);
  return `Pagó ${c.totalConImpuesto}; se retiene el 20 % (${pen}) y se devuelve ${can.datos.valorDevuelto}; habitación Libre; no se cancela dos veces`;
});

// ---------------------------------------------------------------- huésped extranjero (sin IVA)
await caso('N-08', 'Huésped extranjero sin IVA', async () => {
  const h = await habitacionNueva('DOBLE', 200000);
  const ex = await huespedNuevo('Estados Unidos');
  const sin = (await cotizar(h.id, dia(50), dia(51))).datos;
  const con = (await cotizar(h.id, dia(50), dia(51), `&huespedId=${ex.id}`)).datos;
  afirmar(Number(sin.impuesto) > 0, 'sin huésped no se cobra IVA');
  afirmar(igual(con.impuesto, 0) && igual(con.totalConImpuesto, 200000), `extranjero con IVA ${con.impuesto}`);
  const r = await api('recepcion', 'POST', '/reservas', { habitacionId: h.id, huespedId: ex.id, fechaEntrada: dia(50), fechaSalida: dia(51), canal: 'SITIO_WEB',
    abonoInicial: { valor: con.totalConImpuesto, metodo: 'TARJETA_CREDITO' } });
  afirmar(r.status === 201, `reserva de extranjero con pago completo ${r.status} ${JSON.stringify(r.datos).slice(0, 160)}`);
  await api('recepcion', 'PATCH', `/reservas/${r.datos.id}/cancelar`, { motivo: 'Limpieza E2E' });
  return `Cotización sin huésped ${sin.totalConImpuesto} (con IVA); extranjero ${con.totalConImpuesto} (exento); el pago completo se acepta`;
});

// ---------------------------------------------------------------- finanzas: por pagar y recurrentes
await caso('N-09', 'Gastos por pagar', async () => {
  const antes = (await api('finanzas', 'GET', `/finanzas/resumen?desde=${dia(-1)}&hasta=${dia(40)}`)).datos;
  const m = await api('finanzas', 'POST', '/finanzas/movimientos', { tipo: 'EGRESO', categoria: 'SERVICIOS_PUBLICOS', concepto: `Energía E2E ${sufijo}`, valor: 350000,
    fecha: dia(0), tercero: 'Electrohuila', pendiente: true, fechaVencimiento: dia(-2) });
  afirmar(m.status === 201 || m.status === 200, `registrar por pagar ${m.status} ${JSON.stringify(m.datos).slice(0, 150)}`);
  const ing = await api('finanzas', 'POST', '/finanzas/movimientos', { tipo: 'INGRESO', categoria: 'OTROS_INGRESOS', concepto: 'x', valor: 1, pendiente: true });
  const medio = (await api('finanzas', 'GET', `/finanzas/resumen?desde=${dia(-1)}&hasta=${dia(40)}`)).datos;
  afirmar(igual(medio.totalEgresos, antes.totalEgresos), `el egreso por pagar ya cuenta en los totales (${antes.totalEgresos} → ${medio.totalEgresos})`);
  afirmar(igual(medio.porPagar, Number(antes.porPagar) + 350000) && medio.cantidadVencidos >= 1, `por pagar ${antes.porPagar} → ${medio.porPagar}, vencidos ${medio.cantidadVencidos}`);
  const pg = await api('finanzas', 'PATCH', `/finanzas/movimientos/${m.datos.id}/pagar`, { metodoPago: 'TRANSFERENCIA' });
  afirmar(pg.status === 200 && !pg.datos.pendiente, `pagar ${pg.status}`);
  const otra = await api('finanzas', 'PATCH', `/finanzas/movimientos/${m.datos.id}/pagar`, { metodoPago: 'TRANSFERENCIA' });
  afirmar(otra.status >= 400, `se pagó dos veces (${otra.status})`);
  const desp = (await api('finanzas', 'GET', `/finanzas/resumen?desde=${dia(-1)}&hasta=${dia(40)}`)).datos;
  afirmar(igual(desp.totalEgresos, Number(antes.totalEgresos) + 350000) && igual(desp.porPagar, antes.porPagar), `después de pagar egresos ${desp.totalEgresos}, por pagar ${desp.porPagar}`);
  return `Por pagar no suma en egresos hasta pagarse; KPI y vencidos correctos; pagado suma ${350000}; no se paga dos veces; ingreso "por pagar": ${ing.status}`;
});

await caso('N-10', 'Gastos recurrentes', async () => {
  const g = await api('finanzas', 'POST', '/finanzas/gastos-recurrentes', { concepto: `Nómina E2E ${sufijo}`, categoria: 'NOMINA', valor: 2500000, diaVencimiento: 28, tercero: 'Personal', metodoPago: 'TRANSFERENCIA', activo: true });
  afirmar(g.status === 201 || g.status === 200, `crear recurrente ${g.status} ${JSON.stringify(g.datos).slice(0, 150)}`);
  await pausa(500);
  const pend = lista((await api('finanzas', 'GET', `/finanzas/movimientos?pendiente=true&tamano=200`)).datos).filter((m) => m.gastoRecurrenteId === g.datos.id);
  afirmar(pend.length === 1, `pendientes generados para el mes: ${pend.length}`);
  const venc = pend[0].fechaVencimiento;
  const mes = dia(0).slice(0, 7);
  afirmar(venc === `${mes}-28`, `vencimiento ${venc}`);
  const ed = await api('finanzas', 'PUT', `/finanzas/gastos-recurrentes/${g.datos.id}`, { concepto: `Nómina E2E ${sufijo}`, categoria: 'NOMINA', valor: 2600000, diaVencimiento: 28, activo: true });
  const pend2 = lista((await api('finanzas', 'GET', `/finanzas/movimientos?pendiente=true&tamano=200`)).datos).filter((m) => m.gastoRecurrenteId === g.datos.id);
  afirmar(pend2.length === 1, `al editar se duplicó el pendiente del mes (${pend2.length})`);
  const rec = await api('recepcion', 'GET', '/finanzas/gastos-recurrentes');
  afirmar(rec.status === 403, `recepción ve los gastos recurrentes (${rec.status})`);
  return `Se genera 1 egreso por pagar del mes (vence ${venc}); editar (${ed.status}) no lo duplica; valor del pendiente tras editar: ${pend2[0].valor}; recepción 403`;
});

// ---------------------------------------------------------------- reportes
await caso('N-11', 'Reporte con promoción', async () => {
  const h = await habitacionNueva('SUITE', 300000);
  const hu = await huespedNuevo();
  const c = (await cotizar(h.id, dia(0), dia(5), `&promocionId=${promo.id}&huespedId=${hu.id}`)).datos;
  const r = await api('recepcion', 'POST', '/reservas', { habitacionId: h.id, huespedId: hu.id, fechaEntrada: dia(0), fechaSalida: dia(5), canal: 'PRESENCIAL', promocionId: promo.id,
    abonoInicial: { valor: c.totalConImpuesto, metodo: 'EFECTIVO' } });
  afirmar(r.status === 201, `reserva ${r.status}`);
  await api('recepcion', 'PATCH', `/reservas/${r.datos.id}/check-in`);
  const rep = (await api('finanzas', 'GET', `/reportes/ocupacion?desde=${dia(0)}&hasta=${dia(4)}&tipoHabitacion=SUITE`)).datos;
  const porDia = (300000 * 5 - 30000) / 5;
  return `Reporte SUITE: ADR ${rep.adr} (esperado con descuento prorrateado ≈ ${porDia} si es la única suite ocupada), ingreso ${rep.ingresoAlojamiento}`;
});

// ---------------------------------------------------------------- barrido de pantallas por rol
const browser = await navegador();
const RUTAS = {
  admin: ['/inicio', '/habitaciones', '/reservas', '/huespedes', '/personal', '/inventario', '/finanzas', '/facturas', '/reportes', '/promociones', '/usuarios', '/auditoria', '/eventos', '/servicios', '/ayuda'],
  recepcion: ['/inicio', '/habitaciones', '/reservas', '/huespedes', '/servicios', '/eventos', `/reservas/${reservaPromo?.id}`, `/habitaciones/${habs[0]?.id}/reservar`],
  finanzas: ['/inicio', '/finanzas', '/facturas', '/reportes'],
  housekeeping: ['/inicio', '/incidencias', '/habitaciones'],
};
await caso('N-12', 'Pantallas por rol: F5, consola y red', async () => {
  const problemas = [];
  for (const [rol, rutas] of Object.entries(RUTAS)) {
    for (const ruta of rutas) {
      const page = await abrir(browser, rol, '/inicio');
      const errores = [];
      page.on('console', (m) => { if (m.type() === 'error') errores.push(m.text().slice(0, 140)); });
      page.on('response', (r) => { if (r.url().includes('/api/') && r.status() >= 400) errores.push(`${r.status()} ${r.request().method()} ${r.url().replace(/.*\/api/, '/api')}`); });
      await page.goto(`${FRONT}${ruta}`, { waitUntil: 'networkidle0' });   // carga directa = F5 en una ruta profunda
      await pausa(1500);
      const info = await page.evaluate(() => ({ url: location.pathname, texto: document.body.innerText.slice(0, 120) }));
      if (/Not Found/i.test(info.texto)) problemas.push(`${rol} ${ruta}: Not Found`);
      if (info.url !== ruta) problemas.push(`${rol} ${ruta}: redirige a ${info.url}`);
      for (const e of errores) problemas.push(`${rol} ${ruta}: ${e}`);
      await page.screenshot({ path: `${OUT}/${rol}${ruta.replace(/\//g, '_')}.png` });
      await page.close();
    }
  }
  writeFileSync(`${OUT}/barrido.json`, JSON.stringify(problemas, null, 1));
  afirmar(problemas.length === 0, `${problemas.length} problema(s): ${problemas.slice(0, 12).join(' || ')}`);
  return `Todas las rutas cargan directo (F5) sin errores de consola ni respuestas 4xx/5xx`;
});
await browser.close();

writeFileSync(`${OUT}/nuevos.json`, JSON.stringify({ fecha: new Date().toISOString(), resultados }, null, 1));
console.log(`\n${resultados.filter((r) => r.resultado === 'OK').length}/${resultados.length} OK`);
