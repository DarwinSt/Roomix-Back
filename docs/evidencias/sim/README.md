# Validación con gemelos sintéticos

Evidencia de la validación de Roomix con 30 hoteles simulados ("gemelos") de Neiva y Rivera.

## Qué es un gemelo

Es un hotel ficticio dentro de Roomix que tiene el **mismo número de habitaciones y el mismo tipo** que un establecimiento real
inscrito en el Registro Nacional de Turismo (RNT, octubre de 2026). No contiene el nombre ni los datos de contacto del
establecimiento real. Todos los huéspedes, reservas, facturas y pagos son **sintéticos**.

Cada gemelo se creó en una base de datos PostgreSQL propia y vacía. Después, un programa ("recepcionista robot") operó el
hotel durante 60 días **a través de la API de Roomix**: registró huéspedes, creó, canceló y atendió reservas (check-in y check-out),
solicitó servicios, registró pagos e hizo la limpieza de cada habitación. Al final se comparó lo que calcula Roomix con lo que
el programa sabía que había ocurrido.

## Contenido de esta carpeta

| Archivo o carpeta | Qué contiene |
|---|---|
| `gemelos.mjs` | El programa que selecciona los 30 gemelos y simula su operación. |
| `analizar.py` | Resume los resultados y genera los gráficos de la tesis. |
| `resultados/gemelos.json` | Los 30 gemelos elegidos: municipio, tipo, habitaciones y estrato. |
| `resultados/G01.json` … `G30.json` | Resultado de cada gemelo: parámetros, cifras de la simulación, cifras de Roomix, verificaciones de integridad y tiempos de respuesta de cada operación. |
| `resultados/resumen.json` y `resultados/fig/` | Resumen de los 30 gemelos y gráficos. |
| `bases/roomix_sim_gXX.dump` | Copia completa de la base de datos de cada gemelo (formato de `pg_dump`). |
| `credencial-prueba.txt` | Contraseña de prueba de los usuarios de los gemelos (solo sirve en estas bases sintéticas locales). |

Además, las 30 bases siguen disponibles en el PostgreSQL local del equipo con los nombres `roomix_sim_g01` … `roomix_sim_g30`.

## Cómo abrir un gemelo en Roomix

1. Si la base no está en el equipo, restaurarla desde su copia:

   ```bash
   createdb -U postgres roomix_sim_g30
   pg_restore -U postgres -d roomix_sim_g30 bases/roomix_sim_g30.dump
   ```

2. Arrancar el backend apuntando a esa base (desde la carpeta `Roomix-Back`):

   ```bash
   DATABASE_URL=postgresql://postgres:<clave-postgres>@localhost:5432/roomix_sim_g30 APP_SEED_ENABLED=false java -jar target/roomix-0.0.1-SNAPSHOT.jar
   ```

3. Arrancar el frontend (`npm start` en `Roomix-FRONT`), abrir http://localhost:4200 e ingresar con el usuario `admin`
   (o `recepcion`, `finanzas`, `housekeeping`) y la contraseña de `credencial-prueba.txt`.

Se verá un hotel con 60 días de operación: panel del día, reservas, plano por piso, facturas, finanzas y reportes.

## Cómo repetir la validación

```bash
node gemelos.mjs <ruta-al-jar> ../rnt-huila-alojamiento.json resultados 0 30
python analizar.py resultados
```

La selección y la simulación usan una semilla fija (20261102), por lo que se obtienen los mismos gemelos y los mismos datos.

**Ajuste del 2 de noviembre de 2026 (pago obligatorio al reservar).** Desde que Roomix exige registrar el pago al crear una
reserva (códigos RES-015 y RES-016), el script cotiza cada reserva (`/api/reservas/cotizacion`, con el huésped, porque a los
extranjeros no se les cobra IVA) y la crea con su pago: el 85 % paga el total al reservar y el 15 % deja un abono parcial
(el mínimo de la política o la mitad) y paga el saldo en el check-out. La conciliación de caja pasa a ser
*ingresos de Finanzas = pagado en facturas + abonos aún sin facturar* (reservas confirmadas, en curso o canceladas); las
devoluciones de las cancelaciones se registran como egresos. Como el script hace más llamadas al azar, una corrida nueva produce
datos distintos de los de `resultados/` (que corresponden a la versión anterior y se conservan como evidencia).
Para probar sin tocar las bases `roomix_sim_gNN`, defina `ROOMIX_SIM_PREFIJO` (por ejemplo `roomix_prueba_`): la corrida de
prueba de 6 gemelos con este ajuste dio 0 errores, noches y ADR idénticos y la caja cuadrada en los 6.


## Segunda corrida (4 de noviembre de 2026, con el pago obligatorio)

Resultados en `resultados-v2/` y bases `roomix_sim_v2_g01` a `roomix_sim_v2_g30` (las de la primera corrida, `roomix_sim_gNN`, se
conservan sin cambios). 30 gemelos, 520 habitaciones, 7.316 reservas y 103.154 operaciones sobre la API: noches, ADR y RevPAR
coinciden en los 30 gemelos, 0 cruces, 0 errores, 2.563 reservas anticipadas aceptadas en habitaciones no libres y la caja cuadra
en los 30 (ingresos de Finanzas = pagado en facturas + abonos aún sin facturar).

```bash
ROOMIX_SIM_PREFIJO=roomix_sim_v2_ node gemelos.mjs <ruta-al-jar> ../rnt-huila-alojamiento.json resultados-v2 0 30
python analizar.py resultados-v2
```
