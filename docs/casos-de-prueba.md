# Casos de prueba

## 1. Pruebas automatizadas

| Proyecto | Comando | Pruebas | Alcance |
|----------|---------|---------|---------|
| Backend | `./mvnw test` | 49 | Unitarias (JUnit 5 + Mockito) e integración (Spring Boot + MockMvc + H2) |
| Frontend | `npm test` | 13 | Unitarias (Vitest + TestBed) |

**Backend**

| Clase | Qué valida |
|-------|-----------|
| `PasswordPolicyTest` | Longitud mínima, mayúscula, minúscula, número; contraseña temporal |
| `AuthServiceTest` | Login correcto, contraseña errada, bloqueo tras 5 intentos, usuario inactivo, recuperación y restablecimiento |
| `AuditoriaInterceptorTest` | Traducción de rutas a módulo / entidad / acción |
| `EventoServiceTest` | Cruce de horarios en el salón, capacidad, cupo, acceso solo huéspedes, transiciones de estado |
| `ServicioServiceTest` | Transiciones de las solicitudes y bloqueo al cancelar una ya facturada |
| `FinanzasServiceTest` | Registro manual, anulación y bloqueo de anulación de movimientos de pagos, resumen |
| `HabitacionServiceReglasTest` | Deducción de piso y capacidad |
| `SeguridadIntegracionTest` | 401 sin token, 403 por rol, login real y acceso por rol |
| `FlujoHoteleroIntegracionTest` | Reserva con abono → check-in → servicio → check-out → factura con IVA y abono aplicado → pago del saldo → ingreso en finanzas |
| `PersonalYBusquedaIntegracionTest` | Personal operativo y búsqueda de huéspedes sin tildes ni mayúsculas |
| `ReservasAnticipadasYDevolucionIntegracionTest` | H-01: reserva futura en una habitación ocupada sin cruce y activación al salir; H-02: anular una factura pagada devuelve sus pagos y deja saldo $0 |
| `AbonosPromocionesGastosIntegracionTest` | Promoción con IVA sobre el subtotal con descuento; abono mínimo (RES-015) y reserva por cambio de estado bloqueada (RES-016); cancelación con penalidad; gasto recurrente por pagar, actualización del pendiente al editarlo y pago |

**Frontend** (`src/app/core/auth.spec.ts`): sesión guardada y vencida, `authGuard` (redirección y contraseña temporal), `rolGuard`, `invitadoGuard`, interceptor (token Bearer, cierre por 401), menú por rol y política de contraseñas.

## 2. Casos de prueba funcionales (manuales)

Datos: base con los datos de demostración (`APP_SEED_ENABLED=true`). Usuarios de demostración, uno por rol.
Última ejecución: **2 de noviembre de 2026**, automatizada con `docs/evidencias/funcionales/funcionales.mjs` en un ambiente aislado (base nueva `roomix_e2e`). Resultado: **30 OK y 1 parcial** (CP-03 requiere un servidor de correo). Desde el ajuste de pago obligatorio, toda reserva se crea con su pago (ver casos N-03 a N-05).

| ID | Módulo | Rol | Pasos | Resultado esperado | Resultado | Fecha | Responsable |
|----|--------|-----|-------|--------------------|-----------|-------|-------------|
| CP-01 | Acceso | Todos | Ingresar con usuario y contraseña válidos | Entra al inicio con el menú de su rol | OK | 02/11/2026 | Automatizado (E2E) |
| CP-02 | Acceso | — | Ingresar 5 veces con contraseña errada | Mensaje de credenciales inválidas; al 5.º intento la cuenta queda bloqueada 15 min | OK | 02/11/2026 | Automatizado (E2E) |
| CP-03 | Acceso | — | "¿Olvidaste tu contraseña?" → correo → abrir enlace → nueva clave | Se restablece; el enlace no sirve una segunda vez | Parcial: la solicitud responde 200 y un token inválido se rechaza (422); la recepción del correo y … | 02/11/2026 | Automatizado (E2E) |
| CP-04 | Acceso | Admin | Restablecer la clave de un usuario e ingresar con la temporal | El sistema obliga a cambiarla antes de usar la app | OK | 02/11/2026 | Automatizado (E2E) |
| CP-05 | Roles | Housekeeping | Revisar el menú y abrir `/finanzas` escribiendo la URL | Solo ve sus módulos; la URL lo devuelve al inicio | OK | 02/11/2026 | Automatizado (E2E) |
| CP-06 | Usuarios | Admin | Crear un usuario con rol Almacén, desactivarlo e intentar ingresar | Creación correcta; el usuario desactivado no puede ingresar | OK | 02/11/2026 | Automatizado (E2E) |
| CP-07 | Auditoría | Admin | Hacer un check-in y consultar Auditoría filtrando por módulo Reservas | Aparece el registro con usuario, fecha, IP y resultado | OK | 02/11/2026 | Automatizado (E2E) |
| CP-08 | Dashboard | Admin / Recepción / Finanzas | Abrir Inicio | Indicadores, alertas y secciones acordes al rol | OK | 02/11/2026 | Automatizado (E2E) |
| CP-09 | Dashboard | Recepción | Check-in desde "Llegadas de hoy" | La habitación pasa a Ocupada y la llegada sale de la lista | OK | 02/11/2026 | Automatizado (E2E) |
| CP-10 | Reservas | Recepción | Reservar una habitación disponible con canal WhatsApp registrando el pago (completo o abono) | Reserva confirmada; habitación Reservada; total = noches × tarifa | OK | 02/11/2026 | Automatizado (E2E) |
| CP-11 | Reservas | Recepción | Editar fechas de una reserva confirmada a fechas ocupadas | Se rechaza por cruce; con fechas libres se recalcula el total | OK | 02/11/2026 | Automatizado (E2E) |
| CP-12 | Reservas | Recepción | Cancelar una reserva confirmada indicando el motivo | Estado Cancelada con motivo y fecha; habitación liberada | OK | 02/11/2026 | Automatizado (E2E) |
| CP-13 | Reservas | Recepción | Abrir la vista Calendario y seleccionar un día | Muestra las reservas activas de ese día | OK | 02/11/2026 | Automatizado (E2E) |
| CP-14 | Reservas | Recepción | En el detalle pulsar "Comprobante" | Vista de impresión con datos del hotel, estancia y cuenta | OK | 02/11/2026 | Automatizado (E2E) |
| CP-15 | Huéspedes | Recepción | Registrar un huésped sin marcar la autorización de datos | No permite guardar hasta aceptar la autorización (Ley 1581) | OK | 02/11/2026 | Automatizado (E2E) |
| CP-16 | Huéspedes | Recepción | Abrir el detalle de un huésped frecuente | Historial de reservas, servicios y facturas con totales | OK | 02/11/2026 | Automatizado (E2E) |
| CP-17 | Habitaciones | Admin | Crear una habitación indicando piso y capacidad | Se guarda y aparece en el plano en el piso indicado | OK | 02/11/2026 | Automatizado (E2E) |
| CP-18 | Habitaciones | Recepción | Abrir "Plano por piso" | Habitaciones agrupadas por piso, coloreadas por estado | OK | 02/11/2026 | Automatizado (E2E) |
| CP-19 | Habitaciones | Admin | Subir una foto (JPG < 2 MB) y una de 5 MB | La primera se muestra en la galería; la segunda se rechaza | OK | 02/11/2026 | Automatizado (E2E) |
| CP-20 | Inventario | Almacén | Crear un artículo con proveedor y precio | Aparece con su valor total (stock × precio) | OK | 02/11/2026 | Automatizado (E2E) |
| CP-21 | Inventario | Almacén | Registrar una entrada con costo y "Registrar compra" | Stock aumenta, movimiento en el historial, egreso en Finanzas | OK | 02/11/2026 | Automatizado (E2E) |
| CP-22 | Inventario | Almacén | Abrir "Reporte y valorización" | Valor total, críticos y más usados del periodo | OK | 02/11/2026 | Automatizado (E2E) |
| CP-23 | Finanzas | Finanzas | Registrar un egreso de nómina y ver el panel del mes | El egreso se refleja en totales, gráfico y categoría | OK | 02/11/2026 | Automatizado (E2E) |
| CP-24 | Facturación | Recepción | Check-out de una estancia y registrar el saldo en dos pagos | Factura con IVA (o exenta si es extranjero con pasaporte) con el abono de la reserva aplicado; estado Pagada parcial → Pagada | OK | 02/11/2026 | Automatizado (E2E) |
| CP-25 | Facturación | Finanzas | Anular una factura pagada indicando motivo | Estado Anulada y saldo $0; sus pagos quedan devueltos y se registra el egreso de devolución (H-02) | OK | 02/11/2026 | Automatizado (E2E) |
| CP-26 | Servicios | Recepción / Housekeeping | Crear solicitud de room service, asignarla, iniciarla y completarla | Cambios de estado registrados; valor cargado a la cuenta | OK | 02/11/2026 | Automatizado (E2E) |
| CP-27 | Servicios | Recepción | Ver la cuenta de una reserva en curso con servicios | Lista alojamiento + servicios con subtotal, IVA y saldo | OK | 02/11/2026 | Automatizado (E2E) |
| CP-28 | Eventos | Eventos | Crear un evento en un salón y horario ya ocupado | Se rechaza por cruce; en horario libre se crea con sus recursos | OK | 02/11/2026 | Automatizado (E2E) |
| CP-29 | Eventos | Eventos | Inscribir participantes, confirmar y marcar asistencia | Contadores de inscritos, confirmados y asistentes actualizados | OK | 02/11/2026 | Automatizado (E2E) |
| CP-30 | Eventos | Eventos / Finanzas | Iniciar y finalizar el evento; generar su factura | Estados en orden; factura con alquiler y recursos | OK | 02/11/2026 | Automatizado (E2E) |
| CP-31 | Reportes | Finanzas | Elegir "Últimos 30 días" y exportar CSV | KPIs (ocupación, ADR, RevPAR), gráficos y archivo CSV | OK | 02/11/2026 | Automatizado (E2E) |

## 3. Casos E2E de los flujos nuevos

Script `docs/evidencias/e2e/nuevos.mjs` (API + navegador), ejecutado el 2 de noviembre de 2026 sobre la base aislada `roomix_e2e`
con la política de abono mínimo en 50 % y penalidad en 20 %. Resultado: **12/12 OK**. Capturas en `docs/evidencias/e2e/capturas/`.

| ID | Flujo | Resultado | Observación |
|----|-------|-----------|-------------|
| N-01 | Políticas de reserva | OK | Solo Admin la cambia (recepción 403, 150 % → 400); queda abono mínimo 50 % y penalidad 20 % |
| N-02 | Promociones | OK | 5 noches × $200.000 − $30.000 = $970.000 + IVA = 1154300; abono mínimo 577150; no aplica a 2 noches (422) |
| N-03 | Reserva con pago obligatorio | OK | Sin pago, bajo el mínimo, sobre el total y por cambio de estado: rechazados sin dejar reservas; con abono de 577150: confirmada y habitación Reservada |
| N-04 | Abonos de la reserva | OK | Total 1154300 (detalle muestra totalEstimado 970000); 2 abonos visibles; saldo 477150; abono mayor al saldo 422; cada abono es un ingreso en Finanzas |
| N-05 | Check-in → check-out → factura con abonos | OK | Factura FV-000371 = cotización (1154300); abonos aplicados 677150; saldo pagado; Finanzas suma exactamente el total (sin doble conteo) |
| N-06 | Anular factura pagada con abonos | OK | Todos los pagos (incluidos los abonos aplicados) quedan devueltos; egresos de devolución = 1154300; el ingreso de un abono no se anula a mano (422) |
| N-07 | Cancelación con penalidad | OK | Pagó 357000; se retiene el 20 % (71400) y se devuelve 285600; habitación Libre; no se cancela dos veces |
| N-08 | Huésped extranjero sin IVA | OK | Cotización sin huésped 238000 (con IVA); extranjero 200000 (exento); el pago completo se acepta |
| N-09 | Gastos por pagar | OK | Por pagar no suma en egresos hasta pagarse; KPI y vencidos correctos; pagado suma 350000; no se paga dos veces; ingreso "por pagar": 400 |
| N-10 | Gastos recurrentes | OK | Se genera 1 egreso por pagar del mes (vence 2026-11-28); editar (200) no lo duplica; valor del pendiente tras editar: 2600000; recepción 403 |
| N-11 | Reporte con promoción | OK | Reporte SUITE: ADR 387250 (esperado con descuento prorrateado ≈ 294000 si es la única suite ocupada), ingreso 23235000 |
| N-12 | Pantallas por rol: F5, consola y red | OK | Todas las rutas cargan directo (F5) sin errores de consola ni respuestas 4xx/5xx |

### Inconsistencias encontradas en el E2E y corregidas

| ID | Inconsistencia | Corrección | Regresión |
|----|----------------|------------|-----------|
| I-01 | Los movimientos de Finanzas no indicaban qué ingreso venía de un abono; no se podía llegar a la reserva | La API expone `abonoId`; los ingresos por abono y las devoluciones de cancelación tienen el botón «Ver reserva» | N-04, N-05 |
| I-02 | Al editar un gasto recurrente, el egreso por pagar del mes conservaba el valor anterior | El pendiente del mes aún sin pagar toma los datos nuevos (los pagados no cambian) | `AbonosPromocionesGastosIntegracionTest`, N-10 |
| I-03 | Las facturas anuladas mostraban saldo (p. ej. $357.000) aunque «Por cobrar» las excluía | Una factura anulada muestra saldo $0 | `ReservasAnticipadasYDevolucionIntegracionTest`, N-06 |
| I-04 | La columna «Total» de reservas mostraba el valor sin IVA mientras abonos y factura usan el total con IVA | La columna se llama «Alojamiento (sin IVA)» con una ayuda que explica que el IVA y los servicios se suman en la factura | N-12 (barrido) |

Además, la suite verificó que **todas las pantallas cargan directo (F5)** sin «Not Found», errores de consola ni respuestas 4xx/5xx
para Administración, Recepción, Finanzas y Housekeeping (30 rutas).

## 4. Pruebas de compatibilidad

| Dispositivo / navegador | Ancho | Resultado |
|-------------------------|-------|-----------|
| Chrome escritorio | 1366 px | |
| Chrome / Edge escritorio | 1920 px | |
| Tablet (vertical) | 768 px | |
| Teléfono Android (APK) | 360–412 px | |
| iPhone SE (navegador) | 320–375 px | |
