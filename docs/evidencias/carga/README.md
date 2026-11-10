# Prueba de carga de Roomix

Prueba con **Apache JMeter 5.6.3** en modo de línea de comandos contra el gemelo sintético más grande (G30: 87 habitaciones
con 60 días de operación), con el backend y PostgreSQL en el mismo computador portátil. Todos los datos son sintéticos.

## Resultado oficial: segunda ejecución (4 de noviembre de 2026, Roomix 1.1)

Es la que se presenta en la tesis (capítulo de Resultados y Anexo 12). Se ejecutó con el código de Roomix 1.1, que exige el pago
al reservar: cada reserva del escenario lleva un abono de $50.000 en efectivo. Base: `roomix_sim_v2_g30` (segunda corrida de
gemelos). Antes de ejecutarla se cancelaron por la API las reservas que las corridas anteriores dejaron confirmadas.

| Escenario | Usuarios | Muestras | Promedio | P95 | P99 | Error | Peticiones/s |
|---|---|---|---|---|---|---|---|
| Carga normal | 10 | 1.161 | 67 ms | 221 ms | 302 ms | 0 % | 9,0 |
| Carga alta | 50 | 6.328 | 64 ms | 217 ms | 246 ms | 0 % | 42,4 |
| Estrés | 100 | 10.366 | 298 ms | 694 ms | 894 ms | 0,01 % | 69,3 |

Hubo 1 petición fallida de 17.855 (código 422 al crear una reserva): una reserva que el escenario anterior dejó sin cancelar al
terminar su tiempo ocupaba la misma habitación y fecha, y el sistema rechazó correctamente el cruce.

| Archivo | Qué contiene |
|---|---|
| `Roomix-carga.jmx` | Plan de JMeter: 3 grupos de hilos, login de una sola vez, temporizador aleatorio, extractores JSON, aserciones y controladores condicionales. |
| `jmeter-v2/ejecucion/resultados.jtl` | Todas las muestras registradas por JMeter (17.855). |
| `jmeter-v2/ejecucion/reporte/index.html` | Reporte HTML generado por JMeter: abrir en el navegador. |
| `jmeter-v2/ejecucion/resumen-por-escenario.json` | Informe agregado por escenario y por petición calculado desde el `.jtl`. |
| `jmeter-v2/capturas/` | Capturas del reporte HTML (Figuras A12.4 a A12.8 de la tesis). |
| `jmeter_run.sh` | Script que arranca el backend contra la base del gemelo, ejecuta JMeter y genera el reporte HTML y el resumen por escenario. |

## Escenarios

| Escenario | Usuarios | Rampa | Duración |
|---|---|---|---|
| Carga normal | 10 | 10 s | 120 s |
| Carga alta | 50 | 30 s | 120 s |
| Estrés | 100 | 30 s | 120 s |

Cada usuario virtual hace de recepcionista: consulta el panel del día, las habitaciones y la disponibilidad, registra un huésped,
crea una reserva con su abono y la cancela, lista las reservas del día y, cada cinco ciclos, consulta el reporte de ocupación.

## Ejecuciones anteriores (se conservan como evidencia)

| Carpeta o archivo | Qué es | P95 (10 / 50 / 100 usuarios) |
|---|---|---|
| `jmeter/` | Primera ejecución con JMeter (2 de noviembre de 2026), antes del pago obligatorio, sobre `roomix_sim_g30`. Incluye capturas de la interfaz gráfica y el plan auxiliar `Roomix-visor-resultados.jmx`. | 284 / 280 / 1.412 ms, 0 % de error |
| `carga.mjs` y `carga-resultados.json` | Ejecución preliminar con un generador propio en Node.js con la misma estructura del plan. | 134 / 132 / 293 ms, 0 % de error |
| `jmeter-v2-intento-equipo-ocupado/`, `jmeter-v2-intento-con-bateria/` | Intentos descartados: el portátil funcionaba con batería (plan Equilibrado), lo que reduce la frecuencia del procesador. Una medición con un solo usuario confirmó que el código de las dos versiones responde igual sobre la misma base. | — |

En todas las ejecuciones el jar local registraba el SQL en el log (`application-local.properties`, configuración de desarrollo
que la imagen de producción no incluye), por lo que los tiempos son conservadores. Los logs completos del backend
(`backend-jmeter.log`, de 0,7 a 1,2 GB cada uno) no se publican en el repositorio por su tamaño.

## Cómo repetirla

La credencial de prueba de los gemelos no se publica. Para repetir la prueba, cree localmente el archivo
`../sim/credencial-prueba.txt` con la contraseña con la que se generaron los gemelos (variable `ROOMIX_SIM_CLAVE` de
`../sim/gemelos.mjs`); el script la entrega a JMeter mediante un archivo de propiedades temporal, sin mostrarla.

```bash
bash jmeter_run.sh <jmeter-home> <ruta-al-jar> roomix_sim_v2_g30 jmeter-v2
```

Con el generador propio (Node.js 18 o superior y el backend en ejecución):

```bash
ROOMIX_CARGA_CLAVE=<credencial de prueba> node carga.mjs http://localhost:8091 .
```

Los tiempos corresponden a un portátil que ejecuta a la vez la base de datos, el backend y JMeter; en el servidor de producción
(Railway) pueden diferir. No se ejecutó la prueba contra producción para no escribir reservas y huéspedes sintéticos en la base
real del hotel.
