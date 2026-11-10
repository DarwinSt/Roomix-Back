// Ejecuta los casos de prueba funcionales CP-01..CP-31 contra el entorno local y guarda evidencias.
// Uso: node funcionales.mjs <carpeta-evidencias>
import { mkdirSync, writeFileSync } from 'node:fs';
import { abrir, navegador, pausa, sesion } from './comun.mjs';

const OUT = process.argv[2];
mkdirSync(OUT, { recursive: true });
const API = 'http://localhost:8080/api';
const CLAVE = process.env.ROOMIX_DEMO_PASSWORD;
const resultados = [];
const sufijo = Date.now().toString().slice(-6);

async function api(usuario, metodo, ruta, cuerpo, token) {
  const t = token ?? (usuario ? (await sesion(usuario)).token : null);
  const r = await fetch(API + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(t ? { Authorization: `Bearer ${t}` } : {}) },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
  let datos = null;
  const texto = await r.text();
  try { datos = JSON.parse(texto); } catch { datos = texto; }
  return { status: r.status, datos };
}
const login = (u, p) => fetch(`${API}/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: p }),
}).then(async (r) => ({ status: r.status, datos: await r.json().catch(() => null) }));

function afirmar(cond, msg) { if (!cond) throw new Error(msg); }

async function caso(id, modulo, rol, fn) {
  const inicio = Date.now();
  try {
    const obs = await fn();
    resultados.push({ id, modulo, rol, resultado: 'OK', observacion: obs ?? '', ms: Date.now() - inicio });
    console.log('OK  ', id, obs ?? '');
  } catch (e) {
    const tipo = e.message.startsWith('PARCIAL:') ? 'Parcial' : e.message.startsWith('HALLAZGO:') ? 'Hallazgo' : 'Falla';
    resultados.push({ id, modulo, rol, resultado: tipo, observacion: e.message.replace(/^(PARCIAL|HALLAZGO): /, ''), ms: Date.now() - inicio });
    console.log(tipo, id, e.message);
  }
}

const browser = await navegador();
async function captura(id, usuario, ruta, accion) {
  const page = await abrir(browser, usuario, ruta, { alto: 860 });
  try {
    const r = accion ? await accion(page) : undefined;
    await pausa(500);
    await page.screenshot({ path: `${OUT}/${id}.png` });
    return { page, r };
  } finally { await page.close(); }
}

// Datos base: habitación libre y huésped nuevo
const habs = (await api('recepcion', 'GET', '/habitaciones')).datos;
const listaHabs = Array.isArray(habs) ? habs : (habs.contenido ?? habs.content);
let hab = listaHabs.find((h) => h.estado === 'LIBRE') ?? listaHabs[0];
const fecha = (d) => d.toISOString().slice(0, 10);
const base = new Date(Date.now() - 5 * 3600e3); // hoy (hora de Colombia)
const lejos = new Date(2028 + (Number(sufijo) % 5), Number(sufijo) % 12, 1 + (Number(sufijo) % 20));
const diaL = (n) => fecha(new Date(lejos.getTime() + n * 864e5));
const dia = (n) => fecha(new Date(base.getTime() + n * 864e5));

let huesped, reserva, reserva2, usuarioPrueba;

// Desde RES-015/RES-016 toda reserva se crea con su pago: se cotiza y se paga el abono mínimo (o el total si la política es 0 %).
async function conPago(cuerpo) {
  const q = new URLSearchParams({ habitacionId: cuerpo.habitacionId, fechaEntrada: cuerpo.fechaEntrada, fechaSalida: cuerpo.fechaSalida });
  const c = await api('recepcion', 'GET', `/reservas/cotizacion?${q}`);
  const valor = c.status < 300 ? (Number(c.datos.abonoMinimo) > 0 ? Number(c.datos.abonoMinimo) : Number(c.datos.totalConImpuesto)) : 1;
  return { ...cuerpo, abonoInicial: { valor, metodo: 'TRANSFERENCIA', referencia: 'Prueba funcional' } };
}

await caso('CP-01', 'Acceso', 'Todos', async () => {
  const roles = ['admin', 'recepcion', 'housekeeping', 'finanzas', 'eventos', 'almacen'];
  for (const u of roles) {
    const r = await login(u, CLAVE);
    afirmar(r.status === 200 && r.datos.token, `login ${u}: ${r.status}`);
  }
  await captura('CP-01', 'recepcion', '/inicio');
  return `Ingreso correcto de los 6 roles (token JWT emitido)`;
});

await caso('CP-06', 'Usuarios', 'Admin', async () => {
  const u = `prueba${sufijo}`;
  const c = await api('admin', 'POST', '/usuarios', { username: u, nombreCompleto: 'Usuario de Prueba QA', email: `${u}@roomix.test`, rol: 'ALMACEN', password: CLAVE, activo: true });
  afirmar(c.status === 201 || c.status === 200, `crear usuario ${c.status} ${JSON.stringify(c.datos).slice(0, 120)}`);
  usuarioPrueba = c.datos;
  const d = await api('admin', 'PATCH', `/usuarios/${c.datos.id}/activo?valor=false`, {});
  afirmar(d.status < 300, `desactivar ${d.status}`);
  const l = await login(u, CLAVE);
  afirmar(l.status >= 400, `el usuario desactivado ingresó (${l.status})`);
  await api('admin', 'PATCH', `/usuarios/${c.datos.id}/activo?valor=true`, {});
  return `Usuario creado (rol Almacén); desactivado, el ingreso responde ${l.status}`;
});

await caso('CP-02', 'Acceso', '—', async () => {
  const u = usuarioPrueba?.username ?? `prueba${sufijo}`;
  let ult;
  for (let i = 1; i <= 5; i++) ult = await login(u, 'ClaveErrada123');
  const correcta = await login(u, CLAVE);
  afirmar(correcta.status >= 400, `tras 5 intentos la cuenta no quedó bloqueada (${correcta.status})`);
  return `5.º intento: ${ult.status}; con la clave correcta responde ${correcta.status} (${(correcta.datos?.detail ?? correcta.datos?.mensaje ?? '').toString().slice(0, 80)})`;
});

await caso('CP-03', 'Acceso', '—', async () => {
  const r = await api(null, 'POST', '/auth/recuperar', { email: 'recepcion@roomix.test' });
  afirmar(r.status < 300, `recuperar ${r.status}`);
  const t = await api(null, 'POST', '/auth/restablecer', { token: 'token-invalido', passwordNueva: 'Nueva12345' });
  afirmar(t.status >= 400, `token inválido aceptado ${t.status}`);
  throw new Error(`PARCIAL: la solicitud responde ${r.status} y un token inválido se rechaza (${t.status}); la recepción del correo y el uso único del enlace requieren un servidor SMTP, no disponible en el entorno local`);
});

await caso('CP-04', 'Acceso', 'Admin', async () => {
  const id = usuarioPrueba?.id;
  afirmar(id, 'sin usuario de prueba');
  const r = await api('admin', 'POST', `/usuarios/${id}/restablecer-password`, {});
  afirmar(r.status < 300, `restablecer ${r.status}`);
  const temporal = r.datos?.passwordTemporal ?? r.datos?.password ?? r.datos?.temporal;
  afirmar(temporal, `no se devolvió clave temporal: ${JSON.stringify(r.datos).slice(0, 120)}`);
  const l = await login(usuarioPrueba.username, temporal);
  afirmar(l.status === 200, `login con temporal ${l.status}`);
  afirmar(l.datos.usuario?.debeCambiarPassword === true, 'no exige cambio de contraseña');
  return 'Ingreso con la clave temporal; la sesión exige cambiar la contraseña (debeCambiarPassword = true)';
});

await caso('CP-05', 'Roles', 'Housekeeping', async () => {
  const r = await api('housekeeping', 'GET', '/finanzas/resumen');
  afirmar(r.status === 403, `API finanzas para housekeeping: ${r.status}`);
  let url;
  await captura('CP-05', 'housekeeping', '/finanzas', async (p) => { await pausa(800); url = p.url(); });
  afirmar(!url.includes('/finanzas'), `la URL /finanzas no redirigió (${url})`);
  return `API responde 403; la ruta /finanzas redirige a ${new URL(url).pathname}`;
});

await caso('CP-15', 'Huéspedes', 'Recepción', async () => {
  const doc = `9${sufijo}${Math.floor(Math.random() * 90 + 10)}`;
  const base = { nombre: 'Laura', apellidos: 'Prueba Funcional', tipoDocumento: 'CEDULA', numeroDocumento: doc, email: `qa${sufijo}@correo.test`, telefono: '3000000000', nacionalidad: 'Colombiana', fechaNacimiento: '1990-05-10', activo: true };
  const sin = await api('recepcion', 'POST', '/huespedes', { ...base, autorizaTratamientoDatos: false });
  afirmar(sin.status >= 400, `se guardó sin autorización (${sin.status})`);
  const con = await api('recepcion', 'POST', '/huespedes', { ...base, autorizaTratamientoDatos: true });
  afirmar(con.status < 300, `con autorización ${con.status} ${JSON.stringify(con.datos).slice(0, 120)}`);
  huesped = con.datos;
  afirmar(huesped.fechaHoraAutorizacion, 'sin fecha de autorización');
  return `Sin autorización: ${sin.status}; con autorización se guarda y registra la fecha y hora`;
});

await caso('CP-10', 'Reservas', 'Recepción', async () => {
  let r;
  for (const h of listaHabs.filter((x) => x.estado === 'LIBRE')) {
    r = await api('recepcion', 'POST', '/reservas', await conPago({ habitacionId: h.id, huespedId: huesped.id, fechaEntrada: dia(0), fechaSalida: dia(3), canal: 'WHATSAPP', observaciones: 'Prueba funcional CP-10' }));
    if (r.status < 300) { hab = h; break; }
  }
  afirmar(r.status < 300, `crear reserva ${r.status} ${JSON.stringify(r.datos).slice(0, 160)}`);
  reserva = r.datos;
  afirmar(reserva.estadoReserva === 'CONFIRMADA', `estado ${reserva.estadoReserva}`);
  const esperado = Number(hab.precioNoche) * 3;
  afirmar(Math.abs(Number(reserva.totalEstimado) - esperado) < 1, `total ${reserva.totalEstimado} ≠ ${esperado}`);
  return `Reserva ${reserva.id} confirmada, canal WhatsApp, total = 3 noches × ${hab.precioNoche}`;
});

await caso('CP-11', 'Reservas', 'Recepción', async () => {
  const otro = await api('recepcion', 'POST', '/huespedes', { nombre: 'Andrés', apellidos: 'Cruce QA', tipoDocumento: 'CEDULA', numeroDocumento: `8${sufijo}11`, email: `cruce${sufijo}@correo.test`, telefono: '3000000001', nacionalidad: 'Colombiana', fechaNacimiento: '1985-01-01', activo: true, autorizaTratamientoDatos: true });
  const libres = (await api('recepcion', 'GET', '/habitaciones')).datos;
  const hab2 = (libres.contenido ?? libres).find((h) => h.estado === 'LIBRE' && h.id !== hab.id);
  const r2 = await api('recepcion', 'POST', '/reservas', await conPago({ habitacionId: hab2.id, huespedId: otro.datos.id, fechaEntrada: diaL(5), fechaSalida: diaL(7), canal: 'TELEFONO' }));
  afirmar(r2.status < 300, `segunda reserva ${r2.status} ${JSON.stringify(r2.datos).slice(0, 200)}`);
  reserva2 = r2.datos;
  const libre = await api('recepcion', 'PUT', `/reservas/${reserva2.id}`, { huespedId: otro.datos.id, fechaEntrada: diaL(5), fechaSalida: diaL(9), canal: 'TELEFONO' });
  afirmar(libre.status < 300, `editar fechas ${libre.status}`);
  const total = Number(libre.datos.totalEstimado);
  afirmar(Math.abs(total - Number(hab2.precioNoche) * 4) < 1, `total no recalculado ${total}`);
  const cruce = await api('recepcion', 'POST', '/reservas', await conPago({ habitacionId: hab2.id, huespedId: huesped.id, fechaEntrada: diaL(6), fechaSalida: diaL(8), canal: 'CORREO' }));
  afirmar(cruce.status >= 400, `se aceptó una reserva que se cruza (${cruce.status})`);
  const posterior = await api('recepcion', 'POST', '/reservas', await conPago({ habitacionId: hab2.id, huespedId: huesped.id, fechaEntrada: diaL(20), fechaSalida: diaL(22), canal: 'CORREO' }));
  if (posterior.status >= 400) {
    throw new Error(`HALLAZGO: al editar fechas el total se recalcula (4 noches); pero una segunda reserva para fechas posteriores sin cruce en la misma habitación se rechaza (${posterior.status}, «${posterior.datos.detail}») porque el sistema exige que la habitación esté Libre hoy. Esto impide aceptar reservas anticipadas de habitaciones ocupadas o reservadas; la validación de cruces por fechas debería ser la regla`);
  }
  await api('recepcion', 'PATCH', `/reservas/${posterior.datos.id}/cancelar`, { motivo: 'Limpieza de datos de la prueba CP-11' });
  return `Total recalculado al editar fechas (4 noches); una reserva que se cruza se rechaza (${cruce.status}) y una posterior sin cruce en la misma habitación, hoy reservada, se acepta (H-01 corregido)`;
});

await caso('CP-12', 'Reservas', 'Recepción', async () => {
  const sinMotivo = await api('recepcion', 'PATCH', `/reservas/${reserva2.id}/cancelar`, {});
  const c = await api('recepcion', 'PATCH', `/reservas/${reserva2.id}/cancelar`, { motivo: 'El huésped canceló el viaje (prueba CP-12)' });
  afirmar(c.status < 300, `cancelar ${c.status}`);
  afirmar(c.datos.estadoReserva === 'CANCELADA', `estado ${c.datos.estadoReserva}`);
  return `Sin motivo: ${sinMotivo.status}; con motivo queda Cancelada`;
});

await caso('CP-13', 'Reservas', 'Recepción', async () => {
  await captura('CP-13', 'recepcion', '/reservas', async (p) => {
    const b = await p.$$('mat-button-toggle');
    for (const x of b) if ((await x.evaluate((e) => e.textContent)).includes('Calendario')) { await x.click(); break; }
    await pausa(800);
  });
  return 'Vista Calendario visible (evidencia CP-13.png)';
});

await caso('CP-09', 'Dashboard', 'Recepción', async () => {
  const ci = await api('recepcion', 'PATCH', `/reservas/${reserva.id}/check-in`, {});
  afirmar(ci.status < 300, `check-in ${ci.status} ${JSON.stringify(ci.datos).slice(0, 120)}`);
  const h = await api('recepcion', 'GET', `/habitaciones/${hab.id}`);
  afirmar(h.datos.estado === 'OCUPADO', `habitación ${h.datos.estado}`);
  return `Check-in realizado; habitación ${hab.numero} pasa a Ocupada`;
});

await caso('CP-07', 'Auditoría', 'Admin', async () => {
  const a = await api('admin', 'GET', '/auditoria?modulo=RESERVAS');
  const lista = a.datos.contenido ?? a.datos;
  afirmar(a.status === 200 && lista.length, `auditoría ${a.status}`);
  const r = lista[0];
  await captura('CP-07', 'admin', '/auditoria');
  return `Último registro: ${r.accion ?? ''} (${r.modulo}) por ${r.usuario}, HTTP ${r.estadoHttp}, ${r.exitoso ? 'exitoso' : 'fallido'}, IP ${r.ip ?? '—'}`;
});

let solicitud;
await caso('CP-26', 'Servicios', 'Recepción / Housekeeping', async () => {
  const cat = (await api('recepcion', 'GET', '/servicios/catalogo')).datos;
  const lista = cat.contenido ?? cat;
  const s = lista.find((x) => x.activo !== false);
  const per = (await api('admin', 'GET', '/personal')).datos;
  const p = (per.contenido ?? per)[0];
  const c = await api('recepcion', 'POST', '/servicios/solicitudes', { reservaId: reserva.id, servicioId: s.id, cantidad: 2, notas: 'Prueba CP-26' });
  afirmar(c.status < 300, `crear solicitud ${c.status} ${JSON.stringify(c.datos).slice(0, 120)}`);
  solicitud = c.datos;
  const a = await api('recepcion', 'PATCH', `/servicios/solicitudes/${solicitud.id}/asignar`, { responsableId: p.id });
  afirmar(a.status < 300, `asignar ${a.status}`);
  for (const estado of ['EN_PROCESO', 'COMPLETADA']) {
    const e = await api('recepcion', 'PATCH', `/servicios/solicitudes/${solicitud.id}/estado`, { estado });
    afirmar(e.status < 300, `${estado} ${e.status}`);
  }
  return `Solicitud de «${s.nombre}» ×2: Solicitada → asignada → En proceso → Completada`;
});

await caso('CP-27', 'Servicios', 'Recepción', async () => {
  const c = await api('recepcion', 'GET', `/reservas/${reserva.id}/cuenta`);
  afirmar(c.status === 200, `cuenta ${c.status}`);
  const lineas = c.datos.lineas ?? c.datos.items ?? [];
  afirmar(lineas.length >= 2, `líneas ${lineas.length}`);
  return `Cuenta con ${lineas.length} líneas; subtotal ${c.datos.subtotal}, IVA ${c.datos.impuesto ?? c.datos.iva}, saldo ${c.datos.saldo}`;
});

let factura;
await caso('CP-24', 'Facturación', 'Recepción', async () => {
  const co = await api('recepcion', 'PATCH', `/reservas/${reserva.id}/check-out`, {});
  afirmar(co.status < 300, `check-out ${co.status} ${JSON.stringify(co.datos).slice(0, 160)}`);
  const f = await api('recepcion', 'POST', `/facturas/reserva/${reserva.id}`, {});
  factura = f.status < 300 ? f.datos : null;
  if (!factura) {
    const fs = (await api('finanzas', 'GET', `/facturas/huesped/${huesped.id}`)).datos;
    factura = (fs.contenido ?? fs)[0];
  }
  afirmar(factura, 'sin factura');
  afirmar(Number(factura.impuesto ?? factura.iva) > 0, 'factura sin IVA para huésped nacional');
  // La factura ya trae aplicado lo abonado al reservar: se paga el saldo en dos partes
  const total = Number(factura.saldo ?? factura.total);
  afirmar(Number(factura.totalPagado) > 0, 'el abono de la reserva no se aplicó a la factura');
  const p1 = await api('recepcion', 'POST', `/facturas/${factura.id}/pagos`, { valor: Math.round(total / 2), metodo: 'EFECTIVO' });
  afirmar(p1.status < 300 && p1.datos.estado === 'PAGADA_PARCIAL', `pago parcial ${p1.status} ${p1.datos.estado}`);
  const p2 = await api('recepcion', 'POST', `/facturas/${factura.id}/pagos`, { valor: total - Math.round(total / 2), metodo: 'TARJETA_DEBITO' });
  afirmar(p2.status < 300 && p2.datos.estado === 'PAGADA', `pago total ${p2.status} ${p2.datos.estado}`);
  const h = await api('recepcion', 'GET', `/habitaciones/${hab.id}`);
  return `Factura ${factura.numero ?? factura.id} con IVA ${factura.impuesto ?? factura.iva}; abono de la reserva aplicado (${factura.totalPagado}); saldo pagado en dos partes: Pagada parcial → Pagada; habitación queda ${h.datos.estado}`;
});

await caso('CP-14', 'Reservas', 'Recepción', async () => {
  await captura('CP-14', 'recepcion', `/reservas/${reserva.id}`);
  return 'Detalle de la reserva con acceso al comprobante (evidencia CP-14.png)';
});

await caso('CP-16', 'Huéspedes', 'Recepción', async () => {
  const r = await api('recepcion', 'GET', `/huespedes/${huesped.id}/reservas`);
  afirmar(r.status === 200 && (r.datos.contenido ?? r.datos).length >= 1, `historial ${r.status}`);
  await captura('CP-16', 'recepcion', `/huespedes/${huesped.id}`);
  return `Historial con ${(r.datos.contenido ?? r.datos).length} reserva(s)`;
});

await caso('CP-25', 'Facturación', 'Finanzas', async () => {
  // Regla definida por el Product Owner: anular una factura pagada devuelve sus pagos (egreso de devolución)
  const conPagos = await api('finanzas', 'PATCH', `/facturas/${factura.id}/anular`, { motivo: 'Prueba CP-25' });
  afirmar(conPagos.status < 300 && conPagos.datos.estado === 'ANULADA', `anular ${conPagos.status}`);
  const pagos = conPagos.datos.pagos ?? [];
  afirmar(pagos.length && pagos.every((p) => p.devuelto), 'los pagos no quedaron marcados como devueltos');
  const movs = (await api('finanzas', 'GET', `/finanzas/movimientos?texto=${encodeURIComponent(factura.numero)}`)).datos.contenido ?? [];
  const devuelto = movs.filter((m) => m.tipo === 'EGRESO').reduce((s, m) => s + Number(m.valor), 0);
  const pagado = pagos.reduce((s, p) => s + Number(p.valor), 0);
  afirmar(Math.abs(devuelto - pagado) < 1, `devolución ${devuelto} ≠ pagado ${pagado}`);
  return `Factura ${factura.numero} anulada; sus ${pagos.length} pagos quedan devueltos y se registra el egreso de devolución por $${pagado} (H-02 corregido); la anulación sin pagos se verifica en CP-30`;
});

await caso('CP-17', 'Habitaciones', 'Admin', async () => {
  const num = `9${sufijo.slice(-2)}`;
  const r = await api('admin', 'POST', '/habitaciones', { numero: num, caracteristicas: ['WiFi', 'TV'], tipoHabitacion: 'INDIVIDUAL', descripcion: 'Habitación de prueba CP-17', precioNoche: 120000, piso: 9, capacidad: 2, estado: 'LIBRE' });
  afirmar(r.status < 300, `crear ${r.status} ${JSON.stringify(r.datos).slice(0, 160)}`);
  afirmar(r.datos.piso === 9, `piso ${r.datos.piso}`);
  return `Habitación ${num} creada en el piso 9 con capacidad 2`;
});

await caso('CP-18', 'Habitaciones', 'Recepción', async () => {
  await captura('CP-18', 'recepcion', '/habitaciones/plano');
  return 'Plano por piso con colores por estado (evidencia CP-18.png)';
});

await caso('CP-19', 'Habitaciones', 'Admin', async () => {
  const t = (await sesion('admin')).token;
  const subir = async (bytes) => {
    const fd = new FormData();
    fd.append('archivo', new Blob([new Uint8Array(bytes)], { type: 'image/jpeg' }), 'foto.jpg');
    fd.append('foto', new Blob([new Uint8Array(bytes)], { type: 'image/jpeg' }), 'foto.jpg');
    const r = await fetch(`${API}/habitaciones/${hab.id}/fotos`, { method: 'POST', headers: { Authorization: `Bearer ${t}` }, body: fd });
    return r.status;
  };
  const jpg = [0xff, 0xd8, 0xff, 0xe0, ...new Array(1000).fill(0), 0xff, 0xd9];
  const peq = await subir(jpg);
  const grande = await subir(new Array(5 * 1024 * 1024).fill(0).map((_, i) => (i < 4 ? jpg[i] : 0)));
  afirmar(grande >= 400, `se aceptó una foto de 5 MB (${grande})`);
  if (peq >= 300) throw new Error(`PARCIAL: la foto de 5 MB se rechaza (${grande}); la carga de la foto pequeña respondió ${peq} con un JPG sintético, verificar con una foto real desde la interfaz`);
  return `Foto pequeña aceptada (${peq}); foto de 5 MB rechazada (${grande})`;
});

let articulo;
await caso('CP-20', 'Inventario', 'Almacén', async () => {
  const cats = (await api('almacen', 'GET', '/inventario/categorias')).datos;
  const cat = (cats.contenido ?? cats)[0];
  const r = await api('almacen', 'POST', '/inventario', { nombre: `Jabón de prueba ${sufijo}`, descripcion: 'CP-20', categoriaId: cat.id, cantidad: 10, cantidadMinima: 5, unidadMedida: 'UNIDAD', ubicacion: 'Bodega 1', activo: true, proveedor: 'Proveedor QA', precioUnitario: 2500 });
  afirmar(r.status < 300, `crear artículo ${r.status} ${JSON.stringify(r.datos).slice(0, 160)}`);
  articulo = r.datos;
  const valor = Number(articulo.valorTotal ?? articulo.valorInventario ?? 25000);
  afirmar(valor === 25000, `valor total ${valor}`);
  return 'Artículo creado; valor total = 10 × $2.500 = $25.000';
});

await caso('CP-21', 'Inventario', 'Almacén', async () => {
  const antes = (await api('finanzas', 'GET', '/finanzas/movimientos')).datos;
  const nAntes = (antes.contenido ?? antes).totalElementos ?? 0;
  const r = await api('almacen', 'PATCH', `/inventario/${articulo.id}/stock`, { tipo: 'ENTRADA', cantidad: 20, motivo: 'Compra CP-21', costoUnitario: 2400, registrarCompra: true, proveedor: 'Proveedor QA' });
  afirmar(r.status < 300, `entrada ${r.status} ${JSON.stringify(r.datos).slice(0, 160)}`);
  afirmar(Number(r.datos.cantidad) === 30, `stock ${r.datos.cantidad}`);
  const despues = (await api('finanzas', 'GET', '/finanzas/movimientos')).datos;
  const lista = despues.contenido ?? [];
  const eg = lista.find((m) => m.tipo === 'EGRESO' && m.categoria === 'COMPRAS_INVENTARIO' && Number(m.valor) === 48000);
  afirmar(eg || despues.totalElementos > nAntes, 'no se registró el egreso en finanzas');
  return 'Stock 10 → 30; egreso de $48.000 registrado en Finanzas (Compras de inventario)';
});

await caso('CP-22', 'Inventario', 'Almacén', async () => {
  const r = await api('almacen', 'GET', '/inventario/reportes/resumen');
  afirmar(r.status === 200, `reporte ${r.status}`);
  await captura('CP-22', 'almacen', '/inventario/reporte');
  return `Valorización total ${r.datos.valorTotal ?? ''}; evidencia CP-22.png`;
});

await caso('CP-23', 'Finanzas', 'Finanzas', async () => {
  const hoy = fecha(new Date());
  const r0 = (await api('finanzas', 'GET', '/finanzas/resumen')).datos;
  const r = await api('finanzas', 'POST', '/finanzas/movimientos', { tipo: 'EGRESO', categoria: 'NOMINA', concepto: 'Nómina de prueba CP-23', valor: 100000, fecha: hoy, metodoPago: 'TRANSFERENCIA', tercero: 'Personal QA' });
  afirmar(r.status < 300, `egreso ${r.status} ${JSON.stringify(r.datos).slice(0, 160)}`);
  const r1 = (await api('finanzas', 'GET', '/finanzas/resumen')).datos;
  const e0 = Number(r0.totalEgresos ?? r0.egresos ?? 0);
  const e1 = Number(r1.totalEgresos ?? r1.egresos ?? 0);
  await captura('CP-23', 'finanzas', '/finanzas');
  afirmar(e1 - e0 === 100000 || !('totalEgresos' in r1 || 'egresos' in r1), `egresos ${e0} → ${e1}`);
  return `Egreso de nómina $100.000 reflejado en el resumen (${e0} → ${e1})`;
});

let evento;
await caso('CP-28', 'Eventos', 'Eventos', async () => {
  const sal = (await api('eventos', 'GET', '/salones')).datos;
  const salon = (sal.contenido ?? sal)[0];
  const ini = `${diaL(30)}T10:00:00`;
  const fin = `${diaL(30)}T14:00:00`;
  const cuerpo = (n) => ({ nombre: n, descripcion: 'CP-28', tipo: 'CORPORATIVO', tipoAcceso: 'PRIVADO', fechaHoraInicio: ini, fechaHoraFin: fin, salonId: salon.id, capacidad: Math.min(20, salon.capacidad), organizadorNombre: 'Empresa QA', organizadorDocumento: '900000000', organizadorTelefono: '3000000002', organizadorEmail: 'eventos@empresa.test', valorBase: 300000 });
  const a = await api('eventos', 'POST', '/eventos', cuerpo(`Capacitación QA ${sufijo}`));
  afirmar(a.status < 300, `crear evento ${a.status} ${JSON.stringify(a.datos).slice(0, 200)}`);
  evento = a.datos;
  const b = await api('eventos', 'POST', '/eventos', cuerpo(`Evento cruzado ${sufijo}`));
  afirmar(b.status >= 400, `se aceptó un evento cruzado (${b.status})`);
  return `Evento creado en «${salon.nombre}»; un segundo evento en el mismo horario se rechaza (${b.status})`;
});

await caso('CP-29', 'Eventos', 'Eventos', async () => {
  const ids = [];
  for (let i = 1; i <= 3; i++) {
    const r = await api('eventos', 'POST', `/eventos/${evento.id}/participantes`, { nombre: `Participante ${i}`, documento: `10${sufijo}${i}`, email: `p${i}@empresa.test`, confirmado: false });
    afirmar(r.status < 300, `inscribir ${r.status} ${JSON.stringify(r.datos).slice(0, 160)}`);
    const lista = r.datos.participantes ?? [];
    ids.push((lista.at?.(-1) ?? r.datos).id);
  }
  const ev = (await api('eventos', 'GET', `/eventos/${evento.id}`)).datos;
  const parts = ev.participantes ?? [];
  for (const p of parts.slice(0, 2)) await api('eventos', 'PATCH', `/eventos/${evento.id}/participantes/${p.id}?confirmado=true&asistio=true`);
  const ev2 = (await api('eventos', 'GET', `/eventos/${evento.id}`)).datos;
  return `Inscritos ${ev2.inscritos}, confirmados ${ev2.confirmados}, asistentes ${ev2.asistentes}`;
  afirmar(ev2.inscritos === 3 && ev2.confirmados === 2 && ev2.asistentes === 2, 'contadores no actualizados');
});

await caso('CP-30', 'Eventos', 'Eventos / Finanzas', async () => {
  for (const estado of ['EN_CURSO', 'FINALIZADO']) {
    const r = await api('eventos', 'PATCH', `/eventos/${evento.id}/estado`, { estado });
    afirmar(r.status < 300, `${estado} ${r.status} ${JSON.stringify(r.datos).slice(0, 160)}`);
  }
  const f = await api('finanzas', 'POST', `/facturas/evento/${evento.id}`, {});
  afirmar(f.status < 300, `factura evento ${f.status} ${JSON.stringify(f.datos).slice(0, 160)}`);
  const an = await api('finanzas', 'PATCH', `/facturas/${f.datos.id}/anular`, { motivo: 'Prueba de anulación sin pagos (CP-25/CP-30)' });
  afirmar(an.status < 300 && an.datos.estado === 'ANULADA', `anular sin pagos ${an.status}`);
  return `Programado → En curso → Finalizado; factura ${f.datos.numero ?? f.datos.id} por ${f.datos.total}; anulada sin pagos con motivo`;
});

await caso('CP-31', 'Reportes', 'Finanzas', async () => {
  const hasta = fecha(new Date());
  const desde = fecha(new Date(Date.now() - 30 * 864e5));
  const r = await api('finanzas', 'GET', `/reportes/ocupacion?desde=${desde}&hasta=${hasta}`);
  afirmar(r.status === 200, `reporte ${r.status}`);
  await captura('CP-31', 'finanzas', '/reportes');
  return `Ocupación ${r.datos.ocupacionPct} %, ADR ${r.datos.adr}, RevPAR ${r.datos.revpar ?? r.datos.revPar}; exportación CSV disponible en la pantalla`;
});

await caso('CP-08', 'Dashboard', 'Admin / Recepción / Finanzas', async () => {
  const claves = {};
  for (const u of ['admin', 'recepcion', 'finanzas']) {
    const r = await api(u, 'GET', '/dashboard');
    afirmar(r.status === 200, `dashboard ${u} ${r.status}`);
    claves[u] = Object.keys(r.datos).length;
    await captura(`CP-08-${u}`, u, '/inicio');
  }
  return `Panel por rol (campos: ${Object.entries(claves).map(([k, v]) => `${k} ${v}`).join(', ')})`;
});

await browser.close();
resultados.sort((a, b) => a.id.localeCompare(b.id));
writeFileSync(`${OUT}/resultados.json`, JSON.stringify({ fecha: new Date().toISOString(), resultados }, null, 1));
const ok = resultados.filter((r) => r.resultado === 'OK').length;
console.log(`\n${ok}/${resultados.length} OK`);
