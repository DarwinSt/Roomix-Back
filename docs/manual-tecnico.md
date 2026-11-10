# Manual técnico — Roomix

## 1. Arquitectura

```
┌──────────────────────────┐      HTTPS + JWT      ┌───────────────────────────┐      JDBC/SSL      ┌──────────────┐
│ Frontend Angular 21      │ ───────────────────▶ │ Backend Spring Boot 4     │ ─────────────────▶ │ PostgreSQL   │
│ (web estático / APK      │ ◀─────────────────── │ API REST /api/** (Java 21)│ ◀───────────────── │ (Neon)       │
│  Android con Capacitor)  │        JSON           │ Docker en Render          │                    └──────────────┘
└──────────────────────────┘                       └───────────────────────────┘
```

- **Frontend:** Angular 21 standalone, signals, Angular Material, gráficos SVG propios, Capacitor 8 para Android. Sitio estático en Render.
- **Backend:** Spring Boot 4.0, Spring Security 7 (resource server JWT HS256), Spring Data JPA / Hibernate 7, Bean Validation, springdoc-openapi, Actuator. Imagen Docker multi-etapa.
- **Base de datos:** PostgreSQL 14+ (Neon serverless en producción).

### Capas del backend (por módulo)

`controller` (REST + validación) → `service` (reglas de negocio, transacciones) → `repository` (Spring Data, Specifications) → `domain` (entidades JPA). Los DTO son `record` de Java. Los errores de negocio lanzan excepciones con código por módulo, y `GlobalExceptionHandler` las convierte en `ProblemDetail` JSON con su código (`RES-003`, `EVT-002`...).

El núcleo de reservas (`ReservaService`) no conoce a `finanzas`: la coordinación con cobros (crear la reserva con su abono, check-out con factura, cotización) la hacen `ReservaOperacionService` y `ReservaController`, y `finanzas` usa las entidades de `reserva`. Cuando una reserva se cancela, `ReservaService` publica `ReservaCanceladaEvent` y `AbonoService` lo atiende con `@EventListener` (retiene la penalidad y registra la devolución). `@EnableScheduling` activa la generación diaria de los gastos recurrentes.

Módulos: `auth`, `auditoria`, `dashboard`, `habitacion`, `reserva`, `huesped`, `incidencia`, `personal`, `servicio`, `evento`, `inventario`, `finanzas`, `reporte`, `config`, `common`.

### Estructura del frontend

```
src/app
├── core/        config (navegación por rol), guards, interceptors, models, services, utils
├── shared/      gráficos, diálogos, selector de periodo, pipes
├── layout/      shell (menú lateral y barra superior)
└── features/    una carpeta por módulo (auth, home, habitaciones, reservas, huespedes, incidencias,
                 servicios, eventos, inventario, finanzas, reportes, admin, ayuda)
```

## 2. Modelo de datos (tablas principales)

| Tabla | Descripción |
|-------|-------------|
| `usuarios`, `tokens_recuperacion` | Cuentas, rol, hash BCrypt, bloqueo; tokens de recuperación (hash SHA-256) |
| `auditoria` | Bitácora de operaciones |
| `habitaciones`, `habitacion_caracteristicas`, `habitacion_fotos` | Habitaciones (tipo, tarifa, piso, capacidad, estado) y fotos (bytea) |
| `huespedes` | Huéspedes con autorización de datos (Ley 1581) y fecha |
| `reservas`, `tarifas_tipo_habitacion` | Reservas (canal, estados, cancelación, check-in/out) y tarifas por tipo |
| `incidencias`, `incidencia_tareas`, `personal` | Tareas de limpieza y mantenimiento y el personal asignable |
| `servicios_catalogo`, `solicitudes_servicio` | Catálogo y pedidos de servicios cargados a reservas |
| `salones`, `eventos`, `evento_recursos`, `evento_participantes` | Gestión de eventos |
| `inventario_categorias`, `inventario_articulos`, `inventario_movimientos` | Inventario y kardex |
| `facturas`, `factura_items`, `pagos`, `movimientos_financieros` | Facturación, pagos (con marca de devolución) e ingresos y egresos (con `pendiente`, `fecha_vencimiento`, `abono_id`, `gasto_recurrente_id`) |
| `abonos_reserva` | Abonos de cada reserva (valor, medio, referencia, factura a la que se aplicó, devolución) |
| `promociones`, `politica_reserva` | Promociones configurables y la política única de abono mínimo y penalidad por cancelación |
| `gastos_recurrentes` | Gastos fijos mensuales que generan egresos por pagar |

Hibernate crea y ajusta el esquema (`ddl-auto=update`). Las clases `*Migration` (ApplicationRunner) aplican los ajustes que Hibernate no hace solo, como rellenar columnas nuevas en filas existentes.

## 3. Seguridad

- **JWT** firmado con `JWT_SECRET` (HS256), con 8 h de vigencia. En cada petición el rol y el estado activo se leen de la base, así que desactivar un usuario corta su acceso al instante.
- **Autorización** por ruta y método HTTP en `SecurityConfig` (ver tabla de roles en el README del backend). El frontend replica las reglas en `navegacion.config.ts` y en `rolGuard` solo por usabilidad.
- **Contraseñas:** BCrypt, con política de complejidad y bloqueo tras 5 intentos fallidos.
- **CORS** restringido a `CORS_ALLOWED_ORIGINS`.
- **Auditoría** automática de las operaciones de escritura (interceptor MVC, transacción independiente).
- **Datos personales:** autorización obligatoria de tratamiento (Ley 1581 de 2012), registrada con fecha y hora.

## 4. Reglas de negocio destacadas

| Regla | Ubicación |
|-------|-----------|
| No se cruzan reservas en la misma habitación (validación por fechas: se aceptan reservas futuras de habitaciones ocupadas); un huésped no tiene dos reservas que se crucen; check-in solo de la reserva vigente y desde la fecha de llegada | `ReservaService`, `ReservaOperacionService` |
| Toda reserva se crea con su pago: completo o abono ≥ % mínimo del total con IVA (RES-015); el cambio de estado de la habitación no crea reservas (RES-016) | `AbonoService.registrarAbonoInicial`, `HabitacionService` |
| Promoción: una sola por reserva, IVA sobre el subtotal con descuento; el reporte prorratea el descuento por noche | `PromocionService`, `ReservaService`, `ReporteService` |
| Cancelación con abonos: se retiene la penalidad de la política y el resto se devuelve como egreso | `AbonoService.alCancelarReserva` |
| Los abonos son ingresos al recibirse y se aplican como pagos de la factura del check-out, sin volver a contarse | `AbonoService.aplicarAFactura` |
| El check-out factura automáticamente alojamiento y servicios pendientes y deja la habitación en limpieza | `ReservaOperacionService`, `FacturacionService` |
| IVA 19 % sobre alojamiento; exento para extranjeros no residentes con pasaporte (art. 481 E.T.) | `FacturacionService`, `HotelProperties` |
| Cada pago genera ingresos por categoría (alojamiento, servicios, eventos) en finanzas | `FacturacionService.registrarPago` |
| Anular una factura pagada devuelve cada pago (egreso de devolución) y la deja con saldo $0; no se anula a mano un movimiento generado por un pago o un abono | `FacturacionService`, `FinanzasService` |
| Los egresos por pagar no cuentan en los totales hasta confirmar el pago; cada gasto recurrente genera un pendiente por mes, que se actualiza si se edita el gasto antes de pagarlo | `FinanzasService`, `GastoRecurrenteService` |
| Toda variación de stock deja un movimiento; las compras pueden generar un egreso | `InventarioService` |
| Un evento no puede cruzarse con otro en el mismo salón ni superar su capacidad | `EventoService` |
| No se puede desactivar ni degradar al último administrador | `UsuarioService` |

## 5. Configuración y despliegue

Toda la configuración se hace por variables de entorno (ver `Roomix-Back/.env.example` y la tabla del README).

| Componente | Plataforma | Configuración clave |
|------------|-----------|---------------------|
| Base de datos | Neon | Copiar la *connection string* como `DATABASE_URL` |
| Backend | Render (Web Service, Docker) | `DATABASE_URL`, `JWT_SECRET`, `ADMIN_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `FRONTEND_URL`, `APP_SEED_ENABLED` (primer despliegue); health check `/actuator/health` |
| Frontend | Render (Static Site) | Build `npm ci && npm run build`, publicar `dist/roomix/browser`, `API_URL=https://<backend>/api`, rewrite `/* → /index.html` (Redirects/Rewrites). El build también genera `404.html` (`scripts/spa-fallback.mjs`) para que refrescar una ruta profunda no dé «Not Found» |
| APK | Android Studio / Gradle (Capacitor 8; el proyecto no usa el CLI de Ionic) | `API_URL=https://<backend>/api npm run build:android`, luego `cd android && gradlew assembleDebug` (o `npm run apk:debug`) |

### Datos de demostración

Con `APP_SEED_ENABLED=true` se carga `db/seed-data.sql` **solo si no hay habitaciones**. El script se genera con `scripts/generar-seed.py` y usa fechas relativas al día de carga. Para regenerar los datos en una base que ya tiene información, ejecuta el script manualmente con `psql "$DATABASE_URL" -f src/main/resources/db/seed-data.sql` (reemplaza los datos operativos; los usuarios se conservan).

## 6. Pruebas y calidad

- Backend: `./mvnw test` (49 pruebas; H2 en memoria con perfil `test`; cobertura JaCoCo en `target/site/jacoco`).
- Frontend: `npm test` (Vitest) y `npm run build` (compilación estricta de TypeScript y plantillas).
- API: Swagger UI en `/swagger-ui.html` para pruebas manuales de endpoints.
- Funcionales y E2E: `docs/evidencias/funcionales/funcionales.mjs` (31 casos) y `docs/evidencias/e2e/nuevos.mjs` (12 flujos + barrido de pantallas por rol). `docs/evidencias/e2e/servidor.mjs` sirve el build con fallback SPA y reenvía `/api` a un backend aislado.
- Gemelos sintéticos: `docs/evidencias/sim/gemelos.mjs` (30 establecimientos del RNT, 60 días); `ROOMIX_SIM_PREFIJO` permite corridas sin tocar las bases de evidencia.
- Carga: `docs/evidencias/carga/Roomix-carga.jmx` (JMeter 5.6.3, 10/50/100 usuarios).

## 7. Mantenimiento

| Tarea | Cómo |
|-------|------|
| Rotar `JWT_SECRET` | Cambiar la variable y reiniciar: todas las sesiones se cierran |
| Rotar la contraseña de la base | Cambiarla en Neon y actualizar `DATABASE_URL` en Render |
| Respaldo | Neon guarda historial (*point-in-time restore*); también `pg_dump "$DATABASE_URL" > respaldo.sql` |
| Logs | Panel de Render (backend); la auditoría funcional está en el módulo Auditoría |
| Nuevo módulo | Crear el paquete con domain, repository, service y controller, agregar la regla en `SecurityConfig`, la ruta en `app.routes.ts` y el elemento en `navegacion.config.ts` |
