# Contraste: documentación del equipo vs. sistema implementado

Revisión de los tres documentos entregados (`Manual_Usuario_Roomix.pdf`, plantilla `MANUAL DE INSTALACIÓN Y CONFIGURACIÓN DEL SOFTWARE.docx` y `RoomixDocumento-Actualizado.docx`) frente a Roomix 1.0 tal como quedó implementado. Las versiones actualizadas están en `docs/entregables/`.

## 1. Documento de tesis

### Lo que coincide (sin cambios de fondo)
Los ocho módulos de las Tablas 15 a 22 están implementados con sus funcionalidades: autenticación y roles, dashboard, reservas, habitaciones, inventario, finanzas, servicios y eventos. También se cumplen el stack de la Tabla 23 (Java 21, Spring Boot, PostgreSQL, Spring Security + JWT + BCrypt, Neon + Render + Docker, GitHub + DockerHub, Swagger) y los criterios de aceptación de seguridad por rol y trazabilidad.

### Ajustes que se hicieron en la versión 2
| Dónde | Antes | Ahora |
|-------|-------|-------|
| Resumen / Abstract | "escritorio y tablets" | Se agregan teléfonos móviles y la aplicación Android |
| Resultados (intro) | "diseño funcional y resultados esperados" | Resultados del sistema implementado |
| Tablas 15–22 | Solo la tabla de funcionalidades | + párrafo "Implementación en Roomix 1.0" y figuras APA (Figuras 2 a 14) |
| Tabla 23 (stack) | Frontend "Angular + TypeScript + Ionic" | "Angular 21 + TypeScript + Angular Material; app Android con Capacitor (Ionic)" |
| Principios de arquitectura | Responsivo "escritorio y tablets" | Escritorio, tablets y teléfonos, con app Android |
| Nueva sección | — | **Resultados obtenidos**: despliegue, Tabla 25 (pruebas automatizadas), Tabla 26 (estado de indicadores), Figuras 15 y 16 |
| Limitaciones (ahora Tabla 27) | "Sin aplicación móvil nativa" | Existe APK híbrida; la nativa y la publicación en tiendas quedan a futuro |
| Limitaciones (OTA) | — | Se aclara que las reservas OTA se registran con el canal "Agencia / OTA" |
| Discusión | "etapa de guía de tesis" | Implementación cumplida; UAT y SUS como siguiente etapa |
| Conclusiones | — | Párrafo nuevo sobre la implementación (no se borró nada) |
| Anexos y Lista de anexos | "No aplica" | Anexos 1 a 5 (manuales, casos de prueba, UAT, SUS) |
| Listas de tablas y figuras | Hasta Tabla 25 / Figura 1 | Reconstruidas con números de página: 27 tablas y 16 figuras |

### Diferencias que deben decidir ustedes (no se modificaron)
1. **Tabla 18, "Clasificación por tipo":** el documento dice "estándar, superior, suite, familiar, accesible". El sistema maneja individual, doble, triple, suite, familiar y ejecutiva. Opciones: ajustar el texto de la tabla (el párrafo de implementación ya lista los tipos reales) o pedir que se agreguen esos tipos al sistema.
2. **Tabla 17, "Confirmaciones automáticas":** se generan comprobantes **imprimibles o en PDF**, pero no se envían por correo al huésped. Si el documento promete envío automático, falta esa parte.
3. **Tabla 17, "Creación de reservas … servicios adicionales":** los servicios se cargan **después**, durante la estadía, no en el formulario de la reserva.
4. **Tabla 20, "ingresos por … inventario":** el sistema no vende artículos de inventario. Registra **egresos** por compras, no ingresos.
5. **Criterios de aceptación:** "páginas en menos de 3 segundos" y "sin vulnerabilidades críticas en el análisis estático" no tienen evidencia todavía. Se sugiere medir los tiempos con las herramientas del navegador y correr un análisis (por ejemplo SonarCloud u OWASP Dependency-Check) antes de la entrega.
6. **Tabla 14 (Presupuesto):** todos los valores están en $0.
7. **Cronograma (Tablas 12 y 13):** está planteado a 12 meses en 4 fases. Conviene contrastarlo con lo que realmente tomó el desarrollo.
8. **Soporte técnico (10 h diarias, 7 días, línea telefónica):** es un compromiso de servicio, no algo del software. Revisen si lo mantienen.
9. **Marcadores pendientes:** `[código del estudiante]`, `[Facultad]`, `[Nombre del director(a)]`, dedicatoria y agradecimientos.
10. **UAT y SUS:** los instrumentos están listos (`acta-uat.md`, `encuesta-sus.md`), pero no hay resultados. La Tabla 26 los marca como pendientes; al aplicarlos, actualicen esa tabla y la Discusión.

## 2. Manual de usuario

El manual original describía otra versión del sistema. Se reescribió **respetando su estructura** (secciones 1 a 10, portada, "Instrucciones paso a paso", leyendas "Figura. X.Y" y glosario) con capturas nuevas de cada pantalla.

| Original | Realidad actual |
|----------|-----------------|
| "No incluye autenticación de usuarios ni roles" (1.5, 3.1, 3.3, 6.x, 10.1) | Login, 6 roles, recuperación de contraseña, cierre de sesión y auditoría |
| Precio por noche en € (5.3) | Pesos colombianos (COP); se corrigió también en la aplicación |
| Sin finanzas, facturas, servicios, eventos, reportes ni plano | Módulos 5.11–5.18 y 5.4–5.5 agregados |
| "No exponga Roomix a Internet" (10.2) | Está publicado con autenticación; recomendaciones de seguridad actualizadas |
| Acceso en localhost | URL de producción en Render y APK Android |
| Numeración de pasos continua entre secciones (6., 7., …, 35.) | Numeración reiniciada en cada procedimiento |

## 3. Manual de instalación y configuración

La plantilla estaba vacía (`[Especificar]`). Se completaron sus 13 secciones sin cambiar la estructura y se agregaron, dentro de la sección 4, los apartados **4.3 Despliegue en la nube** y **4.4 Generación del APK**. La imagen de ejemplo de configuración se reemplazó por el archivo real de variables de entorno, y se incluyeron capturas de Swagger, del login y del módulo de usuarios.

Pendiente de definir: **licencia** del software (quedó "uso académico", con una nota para ajustarla).

## 4. Archivos generados

| Archivo | Formato |
|---------|---------|
| `entregables/RoomixDocumento-Actualizado-v2.docx` / `.pdf` | Documento de tesis actualizado (79 páginas) |
| `entregables/Manual_Usuario_Roomix_v2.docx` / `.pdf` | Manual de usuario (43 páginas) |
| `entregables/Manual_Instalacion_Configuracion_Roomix.docx` / `.pdf` | Manual de instalación (10 páginas) |
| `capturas/*.png` | 43 capturas (escritorio a 1366 px y móvil a 390 px) |

## 5. Versión 3 del documento de tesis (correcciones del equipo)

Archivo: `entregables/RoomixDocumento-Actualizado-v3.docx` / `.pdf` (87 páginas). Se construyó sobre la v2 nativa y no sobre la copia reenviada por WhatsApp. Esa copia había pasado por una conversión de PDF a Word: 79 secciones, estilos "Body Text" e imágenes flotantes. Las ediciones del equipo se aplicaron todas:

- Códigos de estudiante, programa **Ingeniería de Software**, asesor **Juan Antonio Cuesta Losada**, líneas de "Facultad" eliminadas y "X semestre".
- **Dedicatoria y agradecimientos** redactados, para que el equipo los revise y personalice.
- **Tabla de contenido** completa: preliminares (dedicatoria, agradecimientos, resumen, abstract y listas) y todos los capítulos. Antes mostraba "No se encontraron entradas".
- **Presupuesto real (Tabla 16):** de $8.136.000 a $12.594.000 COP. Equivale a 678 horas de los tres estudiantes valoradas entre $12.000 y $18.000 por hora, con herramientas gratuitas y hosting en plan gratuito o en Render Starter.
- **Resultados de la encuesta** (Metodología → Análisis de datos): ficha técnica, Tabla 11 (perfil), Figuras 2 a 4 (reservas, estado de habitaciones y beneficios esperados) y Tabla 12 (hallazgos → funcionalidades de Roomix). Los datos se presentan agregados y sin nombres (Ley 1581).
- Tablas y figuras renumeradas en todo el documento (ahora Tablas 1–29 y Figuras 1–19), con referencias en el texto, Lista de tablas y Lista de figuras actualizadas.

Observaciones:
- El formulario repite la pregunta de disponibilidad en tiempo real (columnas 7 y 10, con respuestas idénticas); se reportó una sola vez.
- Una respuesta de tamaño indica "Más de 5" habitaciones; se clasificó como "No precisa".
- En la portada dice "título de Ingeniería de Software"; lo usual es "título de **Ingeniero(a) de Software**".
