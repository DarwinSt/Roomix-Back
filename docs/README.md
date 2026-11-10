# Roomix — Documentación del proyecto

Material de soporte para el documento de tesis y la validación del sistema.

| Documento | Contenido |
|-----------|-----------|
| [matriz-requisitos.md](matriz-requisitos.md) | Trazabilidad: requisito del documento → implementación (backend, frontend) → prueba |
| [casos-de-prueba.md](casos-de-prueba.md) | Casos de prueba funcionales por módulo y resumen de las pruebas automatizadas |
| [acta-uat.md](acta-uat.md) | Plan y acta de pruebas de aceptación con usuarios (UAT) por rol |
| [encuesta-sus.md](encuesta-sus.md) | Encuesta de usabilidad SUS (System Usability Scale) y cómo calcular el puntaje |
| [manual-usuario.md](manual-usuario.md) | Manual de usuario por rol (también disponible dentro de la app en **Ayuda**) |
| [contraste-documentacion.md](contraste-documentacion.md) | Qué cambió frente a la documentación original y qué deben decidir los autores |
| [entregables/](entregables) | Documento de tesis v11, manual de usuario v4 y manual de instalación v2 (DOCX y PDF) |
| [evidencias/](evidencias) | Pruebas automatizadas y cobertura, casos funcionales, E2E, gemelos sintéticos (con sus bases restaurables), prueba de carga JMeter, respuestas anonimizadas de la encuesta a hoteles y datos del RNT |
| [diagramas/](diagramas) | Diagramas UML, C4 y de flujo (PlantUML / Mermaid) con sus imágenes |
| [jira/backlog.json](jira/backlog.json) | Copia del backlog de Jira (épicas, sprints e historias HU-01 a HU-69) |
| [capturas/](capturas) | Capturas de pantalla usadas en los documentos |
| [manual-tecnico.md](manual-tecnico.md) | Arquitectura, modelo de datos, seguridad, despliegue y mantenimiento |

Repositorios: `Roomix-Back` (Spring Boot 4, Java 21, PostgreSQL) y `Roomix-FRONT` (Angular 21 + Capacitor para Android).

## Versión documentada

- **Backend:** rama `fix/reservas-anticipadas-devolucion-pagos` (commit `3ffc0cf`), que se integra a `main` mediante pull request. La imagen Docker `sof220241004/roomix` se construye desde ese código con el `Dockerfile` del repositorio.
- **Frontend:** repositorio `Roomix-FRONT`, rama `master` (commit `01c560c`).
- **Despliegue:** backend y PostgreSQL en Railway; frontend estático en Render; APK Android generado con Capacitor.

## Qué no se publica

- Los registros completos del backend durante las pruebas de carga (`backend-jmeter.log`, de 0,7 a 1,2 GB cada uno): superan el límite de GitHub y solo contienen el SQL de depuración.
- La credencial de prueba de los gemelos y de JMeter: los scripts la leen de variables de entorno (`ROOMIX_SIM_CLAVE`, `ROOMIX_DEMO_PASSWORD`, `ROOMIX_CARGA_CLAVE`).
- Las respuestas originales de la encuesta a hoteles, porque contienen el nombre de cada establecimiento (Ley 1581 de 2012). Se publican anonimizadas en `evidencias/encuesta`, sin la respuesta de Ciudad de México, que se excluyó del análisis.
- Las versiones anteriores del documento de tesis.
