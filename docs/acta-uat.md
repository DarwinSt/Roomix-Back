# Plan y acta de pruebas de aceptación de usuario (UAT)

## 1. Objetivo

Verificar con usuarios representativos de cada rol del hotel que Roomix cumple los requisitos funcionales del documento y es apto para la operación diaria.

## 2. Alcance y entorno

- **Versión evaluada:** Roomix v1.1 (backend + frontend web + APK Android), con pago obligatorio al reservar, abonos, promociones, políticas de reserva y gastos por pagar.
- **Entorno:** URL de producción (Render) con datos de demostración, o entorno local.
- **Duración sugerida:** 45–60 minutos por participante.
- **Participantes:** mínimo uno por rol (administración, recepción, housekeeping, finanzas, eventos, almacén). Se recomiendan 5 o más en total para la encuesta SUS.

## 3. Escenarios por rol

| # | Rol | Escenario (tarea a realizar sin ayuda) | Criterio de aceptación |
|---|-----|----------------------------------------|------------------------|
| E1 | Recepción | Registrar a un huésped nuevo y hacerle una reserva de 3 noches por WhatsApp con un abono del 50 % por transferencia | Reserva confirmada con su abono en menos de 3 minutos |
| E2 | Recepción | Hacer el check-in de una llegada del día desde el inicio | Habitación pasa a Ocupada |
| E3 | Recepción | Cargar un servicio de lavandería a una habitación ocupada | Aparece en la cuenta de la reserva |
| E4 | Recepción | Hacer el check-out, revisar la factura (con el abono aplicado) y registrar el saldo con tarjeta | Factura pagada y habitación en limpieza |
| E5 | Housekeeping | Asignarse una limpieza pendiente, iniciarla y finalizarla | Habitación vuelve a Disponible |
| E6 | Finanzas | Registrar un egreso de servicios públicos y consultar la utilidad del mes | Totales actualizados |
| E7 | Finanzas | Generar el reporte de ocupación del último mes y exportarlo | Archivo CSV descargado |
| E8 | Eventos | Crear un evento con recursos, inscribir 3 participantes y marcar asistencia | Contadores correctos |
| E9 | Almacén | Registrar una compra de inventario y verificar las alertas de stock | Stock y alerta actualizados |
| E10 | Administración | Crear un usuario, asignarle un rol y revisar la auditoría | Usuario creado y acción auditada |
| E11 | Administración | Crear la promoción «5 noches o más: $30.000 de descuento» y fijar el abono mínimo en 50 % | Recepción ve la promoción al reservar 5 noches y se exige el abono |
| E12 | Finanzas | Programar el arriendo como gasto recurrente, ver el «Por pagar» y registrar su pago | El egreso pasa de por pagar a pagado y se refleja en los totales |
| E13 | Recepción | Cancelar una reserva con abono | Se muestra la penalidad retenida y la devolución |

## 4. Registro de ejecución

| Escenario | Participante (rol) | Completado (Sí/No) | Tiempo | Errores / ayudas | Observaciones |
|-----------|--------------------|--------------------|--------|------------------|---------------|
| E1 | | | | | |
| E2 | | | | | |
| E3 | | | | | |
| E4 | | | | | |
| E5 | | | | | |
| E6 | | | | | |
| E7 | | | | | |
| E8 | | | | | |
| E9 | | | | | |
| E10 | | | | | |
| E11 | | | | | |
| E12 | | | | | |
| E13 | | | | | |

## 5. Hallazgos

| ID | Escenario | Descripción | Severidad (Alta/Media/Baja) | Acción / responsable | Estado |
|----|-----------|-------------|-----------------------------|----------------------|--------|
| H-01 | | | | | |

## 6. Criterio de aceptación global

El sistema se acepta si: (a) al menos el 90 % de los escenarios se completan sin ayuda, (b) no quedan hallazgos de severidad alta abiertos, y (c) el puntaje SUS promedio es ≥ 68 (ver `encuesta-sus.md`).

## 7. Acta de aceptación

En la ciudad de ____________________, a los ____ días del mes de ____________ de 20____, se reunieron las personas abajo firmantes para evaluar el sistema **Roomix — Software de administración hotelera**.

Resultados:

- Escenarios completados: ____ de ____ (____ %).
- Hallazgos de severidad alta abiertos: ____.
- Puntaje SUS promedio: ____ (participantes: ____).

Decisión: ☐ Aceptado  ☐ Aceptado con observaciones  ☐ No aceptado

Observaciones: ______________________________________________________________

| Nombre | Rol / cargo | Firma |
|--------|-------------|-------|
| | Desarrollador(a) | |
| | Usuario evaluador | |
| | Usuario evaluador | |
| | Director(a) / asesor(a) | |
