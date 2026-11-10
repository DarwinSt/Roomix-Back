# Prueba de carga de Roomix

Prueba ejecutada el 2 de noviembre de 2026 con **Apache JMeter 5.6.3** (modo de línea de comandos) contra el gemelo sintético
más grande (G30: 87 habitaciones con 60 días de operación), con el backend y PostgreSQL en el mismo computador portátil.

## Ejecución con JMeter (resultado oficial)

| Archivo | Qué contiene |
|---|---|
| `Roomix-carga.jmx` | Plan de JMeter: 3 grupos de hilos, login de una sola vez, temporizador aleatorio, extractores JSON, aserciones y controladores condicionales. |
| `jmeter/ejecucion/resultados.jtl` | Todas las muestras registradas por JMeter (15.982). |
| `jmeter/ejecucion/reporte/index.html` | Reporte HTML (dashboard) generado por JMeter: abrir en el navegador. |
| `jmeter/ejecucion/resumen-por-escenario.json` | Informe agregado por escenario y por petición calculado desde el `.jtl`. |
| `jmeter/capturas/` | Capturas del reporte HTML y de la interfaz de JMeter (plan, Aggregate Report y Summary Report). |
| `jmeter/Roomix-visor-resultados.jmx` | Plan auxiliar para abrir `resultados.jtl` en la interfaz de JMeter (Aggregate Report y Summary Report). |

| Escenario | Usuarios | Muestras | Promedio | P95 | P99 | Error | Peticiones/s |
|---|---|---|---|---|---|---|---|
| Carga normal | 10 | 1.143 | 74 ms | 284 ms | 352 ms | 0 % | 8,9 |
| Carga alta | 50 | 6.200 | 91 ms | 280 ms | 350 ms | 0 % | 41,5 |
| Estrés | 100 | 8.639 | 566 ms | 1.412 ms | 1.852 ms | 0 % | 58,0 |

## Ejecución preliminar con el generador propio (Node.js)

## Contenido

| Archivo | Qué contiene |
|---|---|
| `carga.mjs` | Generador de carga con la estructura de un plan de JMeter: grupos de hilos con rampa y duración, login de una sola vez, temporizador aleatorio uniforme (0,5–1,5 s), aserciones de código HTTP e informe agregado. |
| `carga-resultados.json` | Resultados: informe agregado por petición y escenario (muestras, promedio, mediana, P90, P95, P99, mínimo, máximo, % de error y peticiones/s), serie de tiempo cada 5 s y errores. |
| `Roomix-carga.jmx` | El mismo plan para Apache JMeter 5.6. |

## Escenarios

| Escenario | Usuarios | Rampa | Duración |
|---|---|---|---|
| Carga normal | 10 | 10 s | 120 s |
| Carga alta | 50 | 30 s | 120 s |
| Estrés | 100 | 30 s | 120 s |

Cada usuario virtual hace de recepcionista: consulta el panel del día, las habitaciones y la disponibilidad, registra un huésped,
crea una reserva (con un abono de $ 50.000 en efectivo, obligatorio desde el ajuste RES-015 del 2 de noviembre de 2026; la corrida documentada es anterior a ese ajuste) y la cancela, lista las reservas del día y, cada cinco ciclos, consulta el reporte de ocupación.

## Resultado (fila TOTAL)

| Escenario | Muestras | Promedio | P95 | P99 | Error | Peticiones/s |
|---|---|---|---|---|---|---|
| Carga normal | 1.219 | 50 ms | 134 ms | 243 ms | 0 % | 8,9 |
| Carga alta | 6.777 | 44 ms | 132 ms | 160 ms | 0 % | 42,6 |
| Estrés | 12.645 | 114 ms | 293 ms | 386 ms | 0 % | 80,1 |

## Cómo repetirla

Con el generador incluido (requiere Node.js 18 o superior y el backend en ejecución):

```bash
ROOMIX_CARGA_CLAVE=<clave de ../sim/credencial-prueba.txt> node carga.mjs http://localhost:8091 .
```

Con Apache JMeter:

```bash
jmeter -n -t Roomix-carga.jmx -Jhost=localhost -Jport=8091 -Jclave=<clave> -l resultados.jtl -e -o reporte
```


## Segunda ejecución (4 de noviembre de 2026, con el pago obligatorio)

`jmeter-v2/` contiene la ejecución válida, con el equipo conectado a la corriente, contra el gemelo G30 de la segunda corrida
(`roomix_sim_v2_g30`). Antes se cancelaron por la API las reservas que las corridas anteriores dejaron confirmadas.
Percentil 95: 221 ms (10 usuarios), 217 ms (50) y 694 ms (100), frente a 284, 280 y 1.412 ms de la primera ejecución.
Hubo 1 petición fallida de 17.855 (422 al crear una reserva), causada por una reserva que el escenario anterior dejó sin
cancelar al terminar su tiempo en la misma habitación y fecha; el sistema rechazó correctamente el cruce.

Los intentos `jmeter-v2-intento-equipo-ocupado/` y `jmeter-v2-intento-con-bateria/` se descartaron porque el portátil funcionaba
con batería (plan Equilibrado), lo que reduce la frecuencia del procesador. Una medición con un solo usuario confirmó que el
código de las dos versiones responde igual sobre la misma base. En todas las ejecuciones el jar local registra el SQL en el log
(`application-local.properties`), que la imagen de producción no incluye.

```bash
bash jmeter_run.sh <jmeter-home> <ruta-al-jar> roomix_sim_v2_g30 jmeter-v2
```
