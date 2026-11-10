# Diagramas UML de Roomix

Construidos a partir del código y del esquema real de la base de datos. Cada diagrama tiene su fuente editable y la imagen en **PNG** (para Word) y **SVG** (vectorial, no pierde calidad al ampliar).

| # | Diagrama | Fuente | Imagen | Qué muestra |
|---|----------|--------|--------|-------------|
| 1 | Casos de uso | [01-casos-de-uso.puml](01-casos-de-uso.puml) | [casos-de-uso.png](casos-de-uso.png) | Los 6 roles (que heredan de «Usuario autenticado») y 33 casos de uso agrupados por módulo, con relaciones «include»/«extend» |
| 2 | Clases | [02-clases.mmd](02-clases.mmd) | [clases.png](clases.png) | Modelo de dominio: 29 clases en 7 paquetes (incluye promociones, política de reserva, abonos y gastos recurrentes), enumeraciones de estado y multiplicidades |
| 3 | Actividades | [03-actividades.puml](03-actividades.puml) | [actividades.png](actividades.png) | Ciclo completo de una estadía con carriles: Recepción, Sistema, Housekeeping y Finanzas |
| 4 | Estados | [04-estados.puml](04-estados.puml) | [estados.png](estados.png) | Máquinas de estado de Habitación, Reserva (con pago obligatorio y penalidad) y Factura (abonos aplicados y anulación con devolución) |
| 5 | Secuencia | [05-secuencia.puml](05-secuencia.puml) | [secuencia.png](secuencia.png) | Check-out con facturación: frontend → seguridad JWT → servicios → base de datos → auditoría |
| 6 | Entidad-relación | [06-entidad-relacion.mmd](06-entidad-relacion.mmd) | [entidad-relacion.png](entidad-relacion.png) | 29 tablas de PostgreSQL con PK, FK, UK y cardinalidades (incluye abonos, promociones, política de reserva y gastos recurrentes) (notación pata de gallo) |
| 7 | Componentes | [07-componentes.puml](07-componentes.puml) | [componentes.png](componentes.png) | Componentes del frontend Angular y del backend Spring Boot y sus dependencias |
| 8 | Despliegue | [08-despliegue.puml](08-despliegue.puml) | [despliegue.png](despliegue.png) | Producción: navegador/APK, Render (Static Site y Docker), Neon, Docker Hub y GitHub |
| 9 | Contexto (C4 nivel 1) | [09-c4-contexto.puml](09-c4-contexto.puml) | [c4-contexto.png](c4-contexto.png) | Roomix, sus usuarios, el servidor de correo y los entes de control |
| 10 | Contenedores (C4 nivel 2) | [10-c4-contenedores.puml](10-c4-contenedores.puml) | [c4-contenedores.png](c4-contenedores.png) | SPA, app Android, API (monolito modular) y PostgreSQL, con protocolos |
| 11 | Paquetes del backend | [11-paquetes-backend.puml](11-paquetes-backend.puml) | [paquetes-backend.png](paquetes-backend.png) | Módulos por dominio, capas internas y dependencias principales |
| 12 | Estructura del frontend | [12-estructura-frontend.puml](12-estructura-frontend.puml) | [estructura-frontend.png](estructura-frontend.png) | core, layout, features y shared (diseño atómico) |
| 13 | Flujo de autenticación | [13-flujo-autenticacion.puml](13-flujo-autenticacion.puml) | [flujo-autenticacion.png](flujo-autenticacion.png) | Login con BCrypt, emisión del JWT y validación 200/401/403 |
| 14 | Flujo de información | [14-flujo-informacion.puml](14-flujo-informacion.puml) | [flujo-informacion.png](flujo-informacion.png) | Del huésped a la factura, finanzas, reportes y auditoría |

## Regenerar las imágenes

- **PlantUML** (`.puml`, usan el estilo común `_estilo.iuml`):
  `java -jar plantuml.jar -charset UTF-8 -tpng 0*.puml` y `-tsvg` para SVG. También se pueden pegar en https://www.plantuml.com/plantuml.
- **Mermaid** (`.mmd`; clases y entidad-relación, que por su tamaño se dibujan mejor con Mermaid):
  pegarlos en https://mermaid.live o usar `mmdc -i 02-clases.mmd -o clases.svg`.
