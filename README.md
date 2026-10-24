# Roomix — Backend

API REST del sistema de administración hotelera Roomix. Módulos: **autenticación y usuarios (6 roles)**, **auditoría**, **dashboard**, **habitaciones** (estados, plano por piso, fotos, historial), **reservas y tarifas** (canales, edición, cancelación con motivo, check-in/out), **huéspedes** (autorización Ley 1581), **limpieza y mantenimiento**, **personal**, **servicios al huésped**, **eventos y salones**, **inventario** (proveedores, movimientos, valorización), **finanzas y facturación** (IVA 19 %, pagos, egresos) y **reportes** (ocupación, ADR, RevPAR).

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 14+ (local) o una base en Neon / Railway / Render

## Configuración por variables de entorno

Toda la configuración sensible se lee de variables de entorno (ver `.env.example`). En local, si no defines nada, usa `localhost:5432/roomix` con `postgres/postgres`, o los valores de `src/main/resources/application-local.properties` (copia de `application-local.properties.example`, ignorado por git).

| Variable | Obligatoria | Descripción |
|----------|-------------|-------------|
| `DATABASE_URL` | Sí* | URL del proveedor: `postgresql://usuario:clave@host:5432/db?sslmode=require` (Railway, Render, Neon). Se convierte a JDBC automáticamente. |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Sí* | Alternativa a `DATABASE_URL` en formato JDBC (`jdbc:postgresql://...`). |
| `JWT_SECRET` | **Sí (prod)** | Clave para firmar los tokens (32+ caracteres aleatorios). Sin ella se genera una al arrancar y las sesiones se pierden en cada reinicio. |
| `ADMIN_PASSWORD` | **Sí (prod)** | Contraseña inicial del usuario `admin` (solo se usa si no hay usuarios). Sin ella se usa la de demostración, que debe cambiarse. |
| `ADMIN_USERNAME` | No | Usuario administrador inicial (por defecto `admin`). |
| `APP_DEMO_USERS` / `DEMO_USERS_PASSWORD` | No | Crea un usuario por rol (`recepcion`, `housekeeping`, `finanzas`, `eventos`, `almacen`). Por defecto `true`. |
| `JWT_EXPIRATION_HOURS` | No | Duración de la sesión (por defecto 8 h). |
| `FRONTEND_URL` | No | URL del frontend para el enlace de recuperación de contraseña. |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD`, `MAIL_FROM` | No | SMTP para enviar el correo de recuperación. Sin SMTP, el enlace se escribe en el log. |
| `HOTEL_NOMBRE`, `HOTEL_NIT`, `HOTEL_DIRECCION`, `HOTEL_CIUDAD`, `HOTEL_TELEFONO`, `HOTEL_EMAIL` | No | Datos del hotel en facturas y comprobantes. |
| `HOTEL_IVA` / `HOTEL_EXENTO_IVA_EXTRANJEROS` | No | IVA del alojamiento (19 %) y exención para extranjeros no residentes con pasaporte (art. 481 E.T.). |
| `CORS_ALLOWED_ORIGINS` | Sí | URL(s) del frontend separadas por coma. Admite comodines: `https://*.vercel.app`. Incluye `https://localhost` si usarás la app Android. |
| `APP_SEED_ENABLED` | No | `true` carga `db/seed-data.sql` al arrancar **solo si no hay habitaciones**. |
| `PORT` | No | Lo inyecta la plataforma; por defecto `8080`. |
| `APP_TIMEZONE` | No | Zona horaria del hotel (define qué es "hoy"). Por defecto `America/Bogota`. |
| `DB_POOL_SIZE` | No | Conexiones máximas del pool (por defecto `5`, apto para planes gratuitos). |
| `JPA_DDL_AUTO` | No | Por defecto `update` (Hibernate crea/ajusta las tablas). |
| `JPA_SHOW_SQL` | No | `true` para ver SQL en logs. Por defecto `false`. |
| `SWAGGER_ENABLED` | No | `false` oculta Swagger UI. Por defecto `true`. |

\* Usa `DATABASE_URL` **o** el trío `DB_*`.

Health check para la plataforma: `GET /actuator/health`.

## Seguridad

- **Autenticación:** `POST /api/auth/login` devuelve un JWT (HS256) que el frontend envía como `Authorization: Bearer <token>`. Contraseñas con **BCrypt**; bloqueo de 15 min tras 5 intentos fallidos; política de 8+ caracteres con mayúscula, minúscula y número.
- **Recuperación:** `POST /api/auth/recuperar` envía un enlace de un solo uso (30 min, guardado como hash SHA-256) y `POST /api/auth/restablecer` fija la nueva clave.
- **Roles y permisos** (validados en `SecurityConfig` en cada petición contra la base, así un usuario desactivado pierde el acceso al instante):

| Rol | Acceso |
|-----|--------|
| `ADMIN` | Todo, incluidos usuarios y auditoría |
| `RECEPCION` | Reservas, huéspedes, check-in/out, habitaciones, servicios, facturas |
| `HOUSEKEEPING` | Habitaciones (lectura), limpieza y mantenimiento, servicios, inventario (lectura) |
| `FINANZAS` | Finanzas, facturas, reportes, tarifas; lectura de reservas, eventos e inventario |
| `EVENTOS` | Eventos, salones y catálogo de servicios |
| `ALMACEN` | Inventario y movimientos de stock |

- **Auditoría:** un interceptor registra cada operación de escritura (usuario, rol, acción, módulo, entidad, IP, resultado) en la tabla `auditoria`; se consulta en `GET /api/auditoria` (solo ADMIN).
- Errores en JSON (`ProblemDetail`) con códigos por módulo: `AUT-xxx`, `RES-xxx`, `FIN-xxx`, `SRV-xxx`, `EVT-xxx`, `INV-xxx`...

## Base de datos (PostgreSQL)

Las tablas las crea Hibernate (`ddl-auto=update`) al arrancar, y las clases `*SchemaMigration` aplican ajustes adicionales. Solo necesitas una base vacía:

```powershell
psql -U postgres -f src/main/resources/db/init-database.sql
```

### Datos de demostración

`src/main/resources/db/seed-data.sql` contiene un hotel boutique completo en Cartagena (Colombia), con fechas **relativas al día de carga**, así que siempre aparece "en operación":

- 30 habitaciones en 4 pisos, tarifas en COP
- ~120 huéspedes (colombianos y extranjeros) con autorización de datos registrada
- ~410 reservas con canal (presencial, WhatsApp, agencia/OTA...): 75 días de historial, estadías en curso y reservas futuras
- ~370 facturas con IVA y ~370 pagos; ~570 movimientos financieros (ingresos y egresos: nómina, servicios públicos, compras)
- 15 servicios en catálogo y ~280 solicitudes (room service, spa, lavandería, transporte, tours)
- 5 salones, 10 eventos con recursos y ~155 participantes
- 16 personas de personal; incidencias coherentes con el estado de cada habitación
- 48 artículos de inventario con proveedor y precio (algunos bajo el mínimo) y ~460 movimientos de stock
- Usuarios: `admin` + uno por rol (los crea `UsuarioSeed`, no el SQL)

Formas de cargarlo:

1. **Automática (recomendada en la nube):** define `APP_SEED_ENABLED=true` en el primer despliegue. Solo actúa si la tabla `habitaciones` está vacía; después puedes quitar la variable.
2. **Manual:** con el backend arrancado al menos una vez (para que existan las tablas):
   ```bash
   psql "$DATABASE_URL" -f src/main/resources/db/seed-data.sql
   ```
   ⚠️ El script **borra** los datos operativos existentes (habitaciones, huéspedes, reservas, incidencias, personal, artículos).

## Despliegue

El `Dockerfile` (multi-etapa, Java 21) funciona igual en Railway y Render.

### Base de datos

| Opción | Cómo obtener la URL |
|--------|---------------------|
| **Neon** | Dashboard → *Connection string* (incluye `?sslmode=require`). Úsala como `DATABASE_URL`. |
| **Railway** | Añade un servicio PostgreSQL; en el backend referencia `DATABASE_URL=${{Postgres.DATABASE_URL}}`. |
| **Render** | Crea un PostgreSQL y copia la *Internal Database URL* como `DATABASE_URL`. ⚠️ La base gratuita de Render expira a los 30 días. |

### Backend

- **Railway:** *New Service → GitHub repo* (`Roomix-Back`). Detecta el `Dockerfile`. Variables: `DATABASE_URL`, `CORS_ALLOWED_ORIGINS`, `APP_SEED_ENABLED=true`. *Settings → Networking → Generate domain*. Health check: `/actuator/health`.
- **Render:** *New → Web Service → Docker*, mismo repo y variables. Health check path: `/actuator/health`. El workflow `cd-prod.yml` dispara el despliegue con el secreto `RENDER_DEPLOY_HOOK`.
  > El plan gratuito de Render "duerme" el servicio tras 15 min sin tráfico (el primer request tarda ~1 min).

### Orden recomendado

1. Crear la base de datos → copiar `DATABASE_URL`.
2. Desplegar el backend con `APP_SEED_ENABLED=true` y un `CORS_ALLOWED_ORIGINS` provisional.
3. Desplegar el frontend con `API_URL=https://<backend>/api` (ver README del frontend).
4. Actualizar `CORS_ALLOWED_ORIGINS` con la URL real del frontend.

## Arquitectura (capas)

```
com.example.roomix
├── RoomixApplication.java
├── config/              # DATABASE_URL, OpenAPI, datos del hotel, interceptores
├── common/              # Errores globales, paginación, carga de datos demo
├── auth/                # Usuarios, roles, JWT, BCrypt, recuperación de contraseña
├── auditoria/           # Registro automático de operaciones
├── dashboard/           # Indicadores del día por rol
├── habitacion/          # Estados, plano por piso, fotos, historial
├── reserva/             # Reservas, tarifas, check-in/out
├── huesped/             # Huéspedes (Ley 1581)
├── incidencia/          # Limpieza y mantenimiento
├── personal/            # Personal asignable
├── servicio/            # Catálogo y solicitudes de servicios
├── evento/              # Salones, eventos, recursos y participantes
├── inventario/          # Artículos, categorías, movimientos
├── finanzas/            # Facturas, pagos, ingresos y egresos
└── reporte/             # Ocupación, ADR, RevPAR, canales
```

## Ejecutar

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080`.

## Pruebas

```bash
./mvnw test
```

41 pruebas (JUnit 5 + Mockito + MockMvc, base H2 en memoria): reglas de contraseñas, autenticación y bloqueo, auditoría, eventos, servicios, finanzas, habitaciones y dos pruebas de integración (seguridad por rol y flujo hotelero completo reserva → check-in → servicio → check-out → factura → pago).

## Documentación Swagger / OpenAPI

| Recurso | URL |
|---------|-----|
| **Swagger UI** (interactivo) | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8080/api-docs |

La documentación incluye descripciones, ejemplos, códigos de respuesta y esquemas de los DTOs y enums.

## Endpoints — Habitaciones

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/habitaciones` | Listar todas (filtro opcional: `?estado=LIBRE`) |
| GET | `/api/habitaciones/{id}` | Obtener por ID |
| POST | `/api/habitaciones` | Crear |
| PUT | `/api/habitaciones/{id}` | Actualizar completa |
| PATCH | `/api/habitaciones/{id}/estado` | Cambiar solo el estado |
| DELETE | `/api/habitaciones/{id}` | Eliminar |

### Ejemplo — crear habitación

```http
POST /api/habitaciones
Content-Type: application/json

{
  "numero": "101",
  "caracteristicas": ["WiFi", "TV", "Aire acondicionado", "Minibar"],
  "tipoHabitacion": "DOBLE",
  "descripcion": "Habitación doble con vista al jardín",
  "estado": "RESERVADO",
  "fechaHoraReservacion": "2026-05-25T14:30:00"
}
```

Respuesta (campos de auditoría):

```json
{
  "id": 1,
  "numero": "101",
  "estado": "RESERVADO",
  "fechaHoraReservacion": "2026-05-25T14:30:00",
  "fechaHoraUltimaActualizacion": "2026-05-23T15:10:00"
}
```

- `fechaHoraReservacion`: se asigna al pasar a `RESERVADO`. Se mantiene durante `OCUPADO` y mientras la habitación esté `INHABILITADO` por adecuación programada con reserva vigente. Se limpia al pasar a `LIBRE`.
- `motivoInhabilitacion`: obligatorio cuando el estado es `INHABILITADO` (`POST_CHECKOUT` o `ADECUACION_PROGRAMADA`).
- `fechaHoraUltimaActualizacion`: se actualiza sola en cada creación o modificación.

### Estados (`EstadoHabitacion`)

| Estado | Uso típico |
|--------|------------|
| `LIBRE` | Disponible para reservar / asignar |
| `RESERVADO` | Reserva confirmada; huésped sin check-in |
| `OCUPADO` | Huésped en la habitación; reserva en curso |
| `INHABILITADO` | Fuera de servicio; gestionar servicios (incidencias) antes de habilitar |

### Motivo de inhabilitación (`MotivoInhabilitacion`)

Obligatorio al pasar a `INHABILITADO`:

| Motivo | Origen | Destino al habilitar (sin incidencias activas) |
|--------|--------|------------------------------------------------|
| `POST_CHECKOUT` | `OCUPADO` → check-out del huésped | `LIBRE` |
| `ADECUACION_PROGRAMADA` | `LIBRE` o `RESERVADO` → preparación / adecuación | `RESERVADO` si hay reserva vigente, si no `LIBRE` |

### Flujo de estados (habitación)

```mermaid
stateDiagram-v2
    [*] --> LIBRE
    LIBRE --> RESERVADO: reserva confirmada
    LIBRE --> INHABILITADO: ADECUACION_PROGRAMADA
    RESERVADO --> OCUPADO: check-in
    RESERVADO --> LIBRE: cancelación
    RESERVADO --> INHABILITADO: ADECUACION_PROGRAMADA
    OCUPADO --> INHABILITADO: POST_CHECKOUT
    INHABILITADO --> LIBRE: servicios completados (POST_CHECKOUT)
    INHABILITADO --> LIBRE: servicios completados (adecuación, sin reserva)
    INHABILITADO --> RESERVADO: servicios completados (adecuación, con reserva)
```

Las transiciones fuera de este diagrama devuelven **422 Unprocessable Entity**.

En `INHABILITADO` se crean **manualmente** incidencias (limpieza, mantenimiento, servicio al cuarto, otro) con `POST /api/incidencias`. La habitación se **habilita sola** cuando no quedan incidencias activas (al finalizar o cancelar la última):

- `PATCH /api/incidencias/{id}/finalizar` (checklist completo).
- `PATCH /api/incidencias/{id}/cancelar`.

### Tipos (`TipoHabitacion`)

`INDIVIDUAL`, `DOBLE`, `TRIPLE`, `SUITE`, `FAMILIAR`, `EJECUTIVA`

---

## Endpoints — Personal

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/personal` | Listar personal (`?activo=true`) |

Al primer arranque se insertan tres registros de ejemplo (housekeeping) si la tabla está vacía.

---

## Endpoints — Incidencias

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/incidencias` | Listar (`?estado=`, `?habitacionId=`, `?personalId=`, `?activas=true`) |
| POST | `/api/incidencias` | Crear servicio en habitación `INHABILITADO` |
| GET | `/api/incidencias/{id}` | Detalle con tareas (checklist) |
| GET | `/api/incidencias/habitacion/{habitacionId}/activas` | Incidencias activas de una habitación |
| PATCH | `/api/incidencias/{id}/asignar` | Asignar personal → estado `ASIGNADA` (25 %) |
| PATCH | `/api/incidencias/{id}/iniciar` | Iniciar servicio en habitación → `EN_PROGRESO` (50 %) |
| PATCH | `/api/incidencias/{id}/tareas/{tareaId}` | Marcar/desmarcar check del checklist |
| PATCH | `/api/incidencias/{id}/finalizar` | Finalizar incidencia (100 %) y actualizar habitación |
| PATCH | `/api/incidencias/{id}/cancelar` | Cancelar incidencia |

### Tipos (`TipoIncidencia`)

`LIMPIEZA`, `MANTENIMIENTO`, `SERVICIO_CUARTO`, `OTRO` — se crean manualmente en habitaciones `INHABILITADO`.

### Estados (`EstadoIncidencia`)

| Estado | Progreso típico | Descripción |
|--------|-----------------|-------------|
| `CREADA` | 0 % | Incidencia generada; sin personal asignado |
| `ASIGNADA` | 25 % | Personal asignado |
| `EN_PROGRESO` | 50–99 % | Servicio iniciado; avance según checklist |
| `FINALIZADA` | 100 % | Servicio completado; puede habilitar la habitación si no quedan activas |
| `CANCELADA` | — | Incidencia cancelada; puede habilitar la habitación si no quedan activas |

### Flujo de servicios (incidencia + habitación)

```mermaid
sequenceDiagram
    participant H as Habitación
    participant I as Incidencia
    participant P as Personal

    H->>H: OCUPADO → INHABILITADO (POST_CHECKOUT)
    Note over H,I: Operador crea uno o más servicios
    H->>I: POST /api/incidencias (LIMPIEZA, MANTENIMIENTO, …)
    I->>P: PATCH /asignar (25 %)
    P->>I: PATCH /iniciar (50 %)
    loop Checklist
        P->>I: PATCH /tareas/{id} completada
    end
    P->>I: PATCH /finalizar (100 %)
    I->>H: INHABILITADO → LIBRE o RESERVADO (si no quedan activas)
```

### Ejemplo — crear servicio

```json
POST /api/incidencias
{
  "habitacionId": 1,
  "tipo": "LIMPIEZA",
  "descripcion": "Limpieza profunda post check-out"
}
```

### Ejemplo — asignar personal

```json
PATCH /api/incidencias/1/asignar
{
  "personalId": 1
}
```

### Ejemplo — marcar tarea completada

```json
PATCH /api/incidencias/1/tareas/3
{
  "completada": true
}
```

### Errores controlados (`IncidenciaErrorCode`)

| Código | HTTP | Descripción |
|--------|------|-------------|
| `INC-001` | 404 | Incidencia no encontrada |
| `INC-002` | 404 | Tarea no encontrada |
| `INC-003` | 404 | Habitación no encontrada |
| `INC-004` | 404 | Personal no encontrado |
| `INC-005` | 422 | Personal inactivo |
| `INC-006` | 422 | Operación no permitida en el estado actual |
| `INC-007` | 422 | Sin personal asignado |
| `INC-008` | 422 | Tareas incompletas al finalizar |
| `INC-009` | 409 | Ya existe incidencia activa para la habitación |

---

## Endpoints — Inventario

### Categorías (`/api/inventario/categorias`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/inventario/categorias` | Listar (`?soloActivas=true`) |
| GET | `/api/inventario/categorias/{id}` | Detalle |
| POST | `/api/inventario/categorias` | Crear categoría personalizada |
| PUT | `/api/inventario/categorias/{id}` | Actualizar |
| DELETE | `/api/inventario/categorias/{id}` | Eliminar (solo personalizadas, sin artículos) |

**Categorías precargadas** (al primer arranque): Limpieza, Mobiliario, Comida, Ropa blanca, Tecnología, Amenidades, Mantenimiento. Puedes agregar más con POST.

| Categoría | Qué incluye (ejemplos) |
|-----------|-------------------------|
| **Limpieza** | Artículos de aseo, trapeadores, paños, escobas, desinfectante |
| **Mobiliario** | Lo de una habitación: camas, mesas, lámparas, sillas, armarios |
| **Comida** | Restaurante/cocina del hotel: ingredientes, bebidas, utensilios |
| **Ropa blanca** | Toallas, sábanas, fundas |
| **Tecnología** | Routers, cables, controles |
| **Amenidades** | Jabón, shampoo, kit huésped |
| **Mantenimiento** | Herramientas y repuestos |

### Artículos (`/api/inventario`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/inventario` | Listar (`?categoriaId=`, `?activo=`, `?stockBajo=true`) |
| GET | `/api/inventario/{id}` | Obtener por ID |
| POST | `/api/inventario` | Crear artículo |
| PUT | `/api/inventario/{id}` | Actualizar artículo |
| PATCH | `/api/inventario/{id}/stock` | Entrada o salida de stock |
| DELETE | `/api/inventario/{id}` | Baja lógica |

Cada artículo y categoría incluye `fechaHoraCreacion` y `fechaHoraUltimaActualizacion`.

### Ejemplo — crear artículo

```json
POST /api/inventario
{
  "nombre": "Trapeador",
  "descripcion": "Trapeador con cubo incluido",
  "categoriaId": 1,
  "cantidad": 15,
  "cantidadMinima": 5,
  "unidadMedida": "UNIDAD",
  "ubicacion": "Almacén limpieza"
}
```

### Ejemplo — nueva categoría

```json
POST /api/inventario/categorias
{
  "nombre": "Spa",
  "descripcion": "Área de spa y bienestar",
  "ejemplosArticulos": "Toallas spa, aceites esenciales, batas, velas"
}
```

### Ejemplo — salida de stock

```json
PATCH /api/inventario/1/stock
{
  "tipo": "SALIDA",
  "cantidad": 5,
  "motivo": "Reposición habitación 101"
}
```

### Errores controlados (`InventarioErrorCode`)

| Código | HTTP | Descripción |
|--------|------|-------------|
| `INV-001` | 404 | Artículo no encontrado |
| `INV-002` | 409 | Nombre de artículo duplicado |
| `INV-003` | 422 | Artículo inactivo |
| `INV-004` | 422 | Stock insuficiente |
| `INV-005` | 400 | Cantidad mínima inválida |
| `INV-006` | 404 | Categoría no encontrada |
| `INV-007` | 409 | Nombre de categoría duplicado |
| `INV-008` | 422 | Categoría inactiva |
| `INV-009` | 409 | Categoría con artículos (no se puede eliminar) |
| `INV-010` | 422 | Categoría predefinida (no se puede eliminar) |

Respuesta de error (ejemplo):

```json
{
  "type": "about:blank",
  "title": "STOCK_INSUFICIENTE",
  "status": 422,
  "detail": "Stock insuficiente para 'Toalla de baño'. Disponible: 3, solicitado: 5",
  "codigo": "INV-004",
  "modulo": "INVENTARIO"
}
```
