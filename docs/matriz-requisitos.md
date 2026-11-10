# Matriz de trazabilidad de requisitos

Relaciona cada funcionalidad descrita en el documento de tesis con su implementación y la evidencia de prueba.
Leyenda de pruebas: **U** = prueba unitaria, **I** = prueba de integración, **CP** = caso de prueba manual (ver `casos-de-prueba.md`).

## 1. Seguridad y acceso

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Inicio de sesión seguro | `POST /api/auth/login`, `AuthService`, JWT HS256 (`JwtService`) | `/login`, `AuthService`, `auth.interceptor.ts` | U `AuthServiceTest`, I `SeguridadIntegracionTest`, CP-01 |
| Contraseñas cifradas | BCrypt (`PasswordEncoder`) | — | U `AuthServiceTest` |
| Política de contraseñas | `PasswordPolicy` (8+, mayúscula, minúscula, número) | `PATRON_PASSWORD` | U `PasswordPolicyTest`, U `auth.spec.ts` |
| Bloqueo por intentos fallidos | `Usuario` (5 intentos → 15 min) | mensaje AUT-003 | U `AuthServiceTest`, CP-02 |
| Recuperación de contraseña | `/api/auth/recuperar`, `/restablecer`, `TokenRecuperacion` (SHA-256, 30 min), `CorreoService` | `/recuperar`, `/restablecer` | U `AuthServiceTest`, CP-03 |
| Contraseña temporal obligatoria de cambiar | `debeCambiarPassword` | `authGuard` → `/cambiar-password` | U `auth.spec.ts`, CP-04 |
| 6 roles con permisos | `Rol`, `SecurityConfig` (reglas por ruta, rol leído de BD en cada petición) | `rolGuard`, `navegacion.config.ts`, botones por rol | I `SeguridadIntegracionTest`, U `auth.spec.ts`, CP-05 |
| Gestión de usuarios | `/api/usuarios` (CRUD, activar, restablecer clave; protege al último admin) | `/usuarios` | CP-06 |
| Auditoría de acciones | `AuditoriaInterceptor`, `RegistroAuditoria`, `/api/auditoria` | `/auditoria` | U `AuditoriaInterceptorTest`, CP-07 |

## 2. Dashboard

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Indicadores del día según rol | `GET /api/dashboard` (`DashboardService`) | `/inicio` | CP-08 |
| Check-in / check-out rápido | `/api/reservas/{id}/check-in`, `/check-out` | Llegadas y salidas del inicio | I `FlujoHoteleroIntegracionTest`, CP-09 |
| Alertas (no-show, tareas sin asignar, stock bajo, cobros, eventos) | `DashboardService.alertas` | Tarjetas de alerta con enlace | CP-08 |
| Resumen financiero | sección `finanzas` del dashboard | Tarjeta "Resumen financiero" | CP-08 |

## 3. Reservas y huéspedes

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Crear reserva con validación de disponibilidad | `POST /api/reservas`, `ReservaService` | `/habitaciones/:id/reservar` | I `FlujoHoteleroIntegracionTest`, CP-10 |
| Canal de la reserva y observaciones | `CanalReserva`, campos en `Reserva` | Selector de canal | CP-10 |
| Editar reserva | `PUT /api/reservas/{id}` | Diálogo "Editar reserva" | CP-11 |
| Cancelar con motivo | `PATCH /api/reservas/{id}/cancelar` | Diálogo de motivo | CP-12 |
| Calendario de reservas | `GET /api/reservas` | Vista calendario en `/reservas` | CP-13 |
| Comprobante de reserva | — | Botón "Comprobante" (impresión/PDF) en `/reservas/:id` | CP-14 |
| Autorización de datos (Ley 1581 de 2012) | `autorizaTratamientoDatos` obligatorio + fecha | Casilla obligatoria en formularios de huésped | CP-15 |
| Historial del huésped | `/api/huespedes/{id}/reservas`, facturas, servicios | `/huespedes/:id` | CP-16 |
| Varias reservas futuras por habitación (H-01) | `ReservaService` (validación por cruce de fechas, `sincronizarReservaVigente`) | Reserva de habitaciones ocupadas con aviso | I `ReservasAnticipadasYDevolucionIntegracionTest`, CP-11 |
| Pago obligatorio al reservar (completo o abono) | `AbonoService.registrarAbonoInicial` (RES-015), `HabitacionService` (RES-016), `GET /api/reservas/cotizacion` | «Pago de la reserva» en `/habitaciones/:id/reservar` | I `AbonosPromocionesGastosIntegracionTest`, N-03 |
| Abonos de la reserva | `AbonoReserva`, `GET/POST /api/reservas/{id}/abonos` | Sección «Abonos» en `/reservas/:id` | I `FlujoHoteleroIntegracionTest`, N-04, N-05 |
| Promociones configurables | `Promocion`, `/api/promociones`, `/aplicables` | `/promociones` (Admin) y selección al reservar | I `AbonosPromocionesGastosIntegracionTest`, N-02 |
| Abono mínimo y penalidad por cancelación | `PoliticaReserva`, `/api/politicas-reserva`, `AbonoService.alCancelarReserva` | `/promociones` → Políticas de reserva | I `AbonosPromocionesGastosIntegracionTest`, N-01, N-07 |

## 4. Habitaciones

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Estados (disponible, reservada, ocupada, limpieza, mantenimiento) | `EstadoHabitacion` + `MotivoInhabilitacion` | Etiquetas y colores por estado | U `HabitacionServiceReglasTest`, CP-17 |
| Plano por planta | campo `piso` | `/habitaciones/plano` | CP-18 |
| Fotos | `HabitacionFoto`, `/api/habitaciones/{id}/fotos` (máx. 6, 2 MB, JPG/PNG/WebP) | Galería y subida en `/habitaciones/:id` | CP-19 |
| Capacidad | campo `capacidad` | Formulario y detalle | CP-17 |
| Historial de la habitación | `/api/habitaciones/{id}/historial` | Tabla en el detalle | CP-19 |

## 5. Inventario

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Proveedor y precio unitario | campos en `ArticuloInventario` | Formulario de artículo | CP-20 |
| Movimientos (entrada, salida, ajuste) | `MovimientoInventario`, `PATCH /api/inventario/{id}/stock` | Diálogo de stock e historial | CP-21 |
| Compra registrada como egreso | `InventarioService` → `FinanzasService.registrarAutomatico` | Casilla "Registrar la compra" | CP-21 |
| Alertas de stock mínimo | `stockBajo`, dashboard | Inicio y reporte | CP-22 |
| Valorización y más usados | `/api/inventario/reportes/resumen` | `/inventario/reporte` | CP-22 |

## 6. Finanzas

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Ingresos y egresos | `MovimientoFinanciero`, `/api/finanzas/movimientos` | `/finanzas` | U `FinanzasServiceTest`, CP-23 |
| Gráficos y comparativo | `/api/finanzas/resumen` (serie diaria/mensual) | Gráfico de barras y totales por categoría | CP-23 |
| Facturación con IVA 19 % y exención art. 481 E.T. | `FacturacionService` | `/facturas/:id` (imprimible) | I `FlujoHoteleroIntegracionTest`, CP-24 |
| Pagos parciales y totales | `POST /api/facturas/{id}/pagos` | Diálogo de pago | I `FlujoHoteleroIntegracionTest`, CP-24 |
| Anulación con motivo y devolución de pagos (H-02) | `PATCH /api/facturas/{id}/anular` (egreso de devolución por pago, saldo $0) | Botón anular | U `FinanzasServiceTest`, I `ReservasAnticipadasYDevolucionIntegracionTest`, CP-25, N-06 |
| Gastos por pagar | `MovimientoFinanciero.pendiente`, `PATCH /api/finanzas/movimientos/{id}/pagar`, KPI `porPagar` | Casilla «Queda por pagar», filtro Estado, KPI «Por pagar» | I `AbonosPromocionesGastosIntegracionTest`, N-09 |
| Gastos recurrentes mensuales | `GastoRecurrente`, `GastoRecurrenteService` (`@Scheduled`), `/api/finanzas/gastos-recurrentes` | Diálogo «Gastos recurrentes» | I `AbonosPromocionesGastosIntegracionTest`, N-10 |
| Trazabilidad abono → reserva | `MovimientoResponse.abonoId`, referencia `RES-n` | Botón «Ver reserva» en movimientos | N-04 |

## 7. Servicios al huésped

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Catálogo | `ServicioCatalogo`, `/api/servicios/catalogo` | Vista "Catálogo" | CP-26 |
| Solicitudes y programación | `SolicitudServicio` (asignar, programar, estados) | Vista "Solicitudes" | U `ServicioServiceTest`, CP-26 |
| Cargo a la cuenta de la reserva | `FacturacionService.cuentaReserva` | Cuenta en `/reservas/:id` | I `FlujoHoteleroIntegracionTest`, CP-27 |
| Historial | filtros por reserva / huésped | Detalle de reserva y huésped | CP-16 |

## 8. Eventos

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Salones y cruce de horarios | `Salon`, `EventoService` (valida cruce y capacidad) | `/eventos` → Salones | U `EventoServiceTest`, CP-28 |
| Participantes, confirmación y asistencia | `EventoParticipante` | `/eventos/:id` | U `EventoServiceTest`, CP-29 |
| Tipo de acceso (público, privado, solo huéspedes) | `TipoAcceso` | Formulario y diálogo de inscripción | U `EventoServiceTest`, CP-29 |
| Estados (programado, en curso, finalizado, cancelado) | transiciones en `EventoService` | Botones Iniciar/Finalizar/Cancelar | U `EventoServiceTest`, CP-30 |
| Recursos y logística | `EventoRecurso` | Filas de recursos en el formulario | CP-28 |
| Facturación y reporte | `facturarEvento`, `/api/eventos/reporte` | Botón "Generar factura", sección en Reportes | CP-30 |

## 9. Reportes y analítica

| Requisito | Backend | Frontend | Prueba |
|-----------|---------|----------|--------|
| Ocupación, ADR, RevPAR, cancelaciones, estancia promedio | `ReporteService.ocupacion` | `/reportes` | CP-31 |
| Canales, tipos de habitación, procedencia, servicios | ídem | Gráficos de barras | CP-31 |
| Exportación | — | CSV (Excel) e impresión/PDF | CP-31 |

## 10. Requisitos no funcionales

| Requisito | Cómo se cumple |
|-----------|----------------|
| Responsive / móvil | Diseño adaptable 320–1920 px (tablas convertidas en tarjetas, KPIs en 2 columnas), APK Android con Capacitor |
| Despliegue en la nube | Docker (Render), PostgreSQL (Neon), sitio estático (Render); configuración 100 % por variables de entorno |
| Disponibilidad / monitoreo | `GET /actuator/health` |
| Documentación de la API | Swagger UI (`/swagger-ui.html`) |
| Mantenibilidad | Capas por módulo, códigos de error por módulo, pruebas automatizadas (49 backend + 13 frontend), E2E (31 + 12 casos) |
| Recarga de página (F5) en el despliegue | `scripts/spa-fallback.mjs` genera `404.html`; regla de reescritura `/*` → `/index.html` en Render |
| Exactitud y rendimiento | 30 gemelos sintéticos del RNT (Neiva y Rivera) y prueba de carga con JMeter (`docs/evidencias/sim`, `docs/evidencias/carga`) |
| Protección de datos | Ley 1581 (autorización), contraseñas BCrypt, tokens de recuperación con hash, auditoría |
