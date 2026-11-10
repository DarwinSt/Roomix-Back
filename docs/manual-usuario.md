# Manual de usuario — Roomix

Roomix es el sistema de administración del hotel. Funciona en el navegador (computador, tablet o teléfono) y como aplicación Android. Cada persona entra con su propio usuario y ve solo los módulos de su rol. La misma guía está dentro de la aplicación en **Ayuda**.

## 1. Ingreso

1. Abre la dirección del sistema o la app Android.
2. Escribe tu **usuario** y **contraseña** y pulsa **Ingresar**.
3. Si es tu primer ingreso o el administrador restableció tu clave, el sistema te pedirá una nueva: mínimo 8 caracteres, con mayúscula, minúscula y número.
4. ¿Olvidaste la contraseña? Pulsa **¿Olvidaste tu contraseña?**, escribe tu correo y abre el enlace que recibirás (vale 30 minutos).
5. Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.
6. Al terminar el turno usa **Cerrar sesión** en el menú de tu nombre (arriba a la derecha).

## 2. Inicio (panel del día)

Muestra lo importante del día según tu rol:

- **Alertas:** reservas sin check-in, tareas sin responsable, inventario bajo el mínimo, facturas por cobrar, eventos de hoy. Al tocarlas vas al módulo correspondiente.
- **Ocupación:** porcentaje del día y habitaciones disponibles, reservadas, ocupadas, en limpieza y en mantenimiento.
- **Llegadas y salidas de hoy:** botones de **Check-in** y **Check-out** en un toque.
- **Resumen financiero**, **operación**, **inventario crítico** y **próximos eventos** (según el rol).

## 3. Recepción

### Hacer una reserva
1. Ve a **Habitaciones**, elige la habitación y pulsa **Reservar**. También se pueden reservar fechas futuras de una habitación que hoy está ocupada o reservada, siempre que no se crucen con otra estadía.
2. Escribe los datos del huésped (si ya estuvo antes, búscalo por nombre o documento y se reutiliza su ficha).
3. Elige fechas, **canal** (presencial, teléfono, WhatsApp, correo, sitio web, agencia) y observaciones.
4. Si hay **promociones** que aplican (por ejemplo, 5 noches o más), escoge una; el total se recalcula.
5. En **Pago de la reserva** revisa el total con IVA y registra el pago, que es **obligatorio**:
   - **Pago completo:** el valor queda fijo en el total de la reserva.
   - **Abono parcial:** escribe el valor; debe ser al menos el **abono mínimo** de la política del hotel (por ejemplo, 50 %). El saldo se paga en el check-out.
   - Elige el medio de pago y, si quieres, la referencia (número de aprobación, comprobante de transferencia).
6. Marca la **autorización de tratamiento de datos** (obligatoria por la Ley 1581 de 2012).
7. Confirma: la reserva queda **Confirmada** y se abre su detalle. Sin pago el sistema no deja confirmar.

> Cambiar el estado de una habitación a **Reservada** ya no crea reservas: toda reserva se hace con **Reservar** para que quede su pago.

### Gestionar una reserva
En **Reservas** (listado o calendario) abre la reserva para:
- **Editar** fechas, canal u observaciones.
- Ver los **Abonos**: total de la reserva, abono mínimo, abonado y saldo, con la lista de pagos. Con **Registrar abono** se agrega otro pago (no puede superar el saldo).
- **Cancelar** indicando el motivo. Si la reserva tiene abonos, el sistema muestra antes la **penalidad** que se retiene según la política del hotel y lo que se devuelve al huésped.
- **Check-in** (desde el día de llegada) y **Check-out**.
- **Cargar servicio** a la cuenta, ver la **cuenta** y el saldo.
- Imprimir el **Comprobante** (o guardarlo como PDF).

### Check-out y factura
Al hacer check-out se genera la factura con alojamiento (con la promoción, si la hubo), servicios e IVA del 19 %. Los extranjeros no residentes con pasaporte quedan exentos. Los **abonos** hechos al reservar aparecen ya como pagos de la factura; desde la factura registra el **saldo** (efectivo, tarjeta, transferencia...), que puede pagarse en partes. La habitación pasa a **En limpieza**.

> En el listado de Reservas, la columna **Alojamiento (sin IVA)** muestra noches × tarifa menos la promoción; el IVA y los servicios se suman en la cuenta y la factura.

## 4. Housekeeping (limpieza y mantenimiento)

1. En **Limpieza y mantenimiento** verás las tareas pendientes (las de limpieza se crean solas al hacer check-out).
2. Asigna la tarea a una persona, pulsa **Iniciar** y marca la lista de chequeo.
3. Al **Finalizar**, la habitación vuelve a estar **Disponible**.
4. En **Servicios** puedes atender las solicitudes de los huéspedes (room service, lavandería...).

## 5. Servicios al huésped

- **Solicitudes:** recepción registra el pedido sobre una reserva en curso; el valor se carga a la cuenta. El personal asigna un responsable, puede programar la hora, lo **inicia** y lo **completa**. Si se cancela (con motivo) el cargo se retira, salvo que ya esté facturado.
- **Catálogo:** coordinador de eventos y administración definen servicios, precios y unidades.

## 6. Eventos

1. En **Eventos → Nuevo evento** indica nombre, tipo, acceso (público, privado o solo huéspedes), horario, salón, capacidad, organizador y recursos (audiovisuales, mobiliario, catering...). El valor del salón se calcula con su tarifa por hora.
2. El sistema impide reservar un salón ocupado o superar su capacidad.
3. En el detalle inscribe participantes (huéspedes o externos), marca **confirmado** y **asistió**, y exporta la lista a CSV.
4. Cambia el estado: **Iniciar** → **Finalizar** (o **Cancelar** con motivo).
5. Finanzas genera la **factura del evento**.
6. En **Salones** se administran los espacios y sus tarifas.

## 7. Inventario (almacén)

- Cada artículo tiene categoría, stock mínimo, proveedor, precio y ubicación.
- **Movimiento:** **Entrada** (compra, con costo y proveedor; puede registrarse como egreso en Finanzas), **Salida** (consumo) o **Ajuste** (conteo físico).
- El ícono de historial muestra todos los movimientos del artículo.
- **Reporte y valorización:** valor total, valor por categoría, artículos más usados, críticos y movimientos del periodo.

## 8. Finanzas

- **Panel:** ingresos, egresos y utilidad del periodo con comparación con el periodo anterior, gráfico diario o mensual y totales por categoría.
- **Registrar egreso o ingreso manual** (nómina, servicios públicos, proveedores...). Los ingresos por pagos de facturas y por abonos de reservas se registran solos; en los abonos, el botón **Ver reserva** lleva a la reserva que los originó.
- **Gastos por pagar:** al registrar un egreso marca **Queda por pagar** e indica el vencimiento. No cuenta en los egresos hasta que pulses **Registrar pago**. El indicador **Por pagar** muestra el total, la cantidad y los vencidos; el filtro **Estado → Por pagar** los lista.
- **Gastos recurrentes:** con el botón **Gastos recurrentes** programa los gastos fijos del mes (arriendo, nómina, servicios públicos) con su día de vencimiento (1 a 28). Cada mes el sistema crea solo el egreso por pagar; si editas el gasto, el pendiente del mes que aún no se ha pagado toma los datos nuevos.
- **Facturas:** búsqueda, detalle imprimible, pagos y anulación con motivo. Al anular una factura pagada, sus pagos quedan **devueltos** y se registra el egreso de devolución; la factura anulada queda con saldo $0.
- **Reportes:** ocupación, ADR (tarifa promedio), RevPAR (ingreso por habitación disponible), cancelaciones, canales, procedencia, servicios y eventos. Se exportan a CSV o se imprimen.

## 9. Administración

- **Usuarios:** crear, editar, activar/desactivar y restablecer contraseñas (se genera una clave temporal). Roles: Administrador, Recepción, Housekeeping, Finanzas, Eventos y Almacén.
- **Auditoría:** quién hizo qué, cuándo, desde qué IP y con qué resultado. Se filtra por usuario, módulo y fechas.
- **Habitaciones:** alta y edición (tipo, tarifa, piso, capacidad, características) y fotos (hasta 6, de máximo 2 MB).
- **Promociones:** crea descuentos de valor fijo o porcentaje con condiciones (mínimo de noches, tipo de habitación, canal y fechas de llegada), y actívalos o desactívalos. Recepción solo ve las que aplican a cada reserva.
- **Políticas de reserva** (en la misma pantalla): **abono mínimo para reservar** (% del total con IVA; 0 = basta cualquier pago) y **penalidad por cancelación** (% de lo abonado que se retiene; el resto se devuelve).

## 10. Preguntas frecuentes

| Pregunta | Respuesta |
|----------|-----------|
| No veo un módulo | Tu rol no tiene acceso; pide al administrador que revise tu rol. |
| "Tu sesión expiró" | La sesión dura 8 horas; vuelve a ingresar. |
| La primera carga tarda | En el plan gratuito el servidor se suspende sin uso; la primera petición tarda cerca de un minuto. |
| ¿Cómo guardo una factura en PDF? | Abre la factura → **Imprimir** → destino "Guardar como PDF". |
| No me deja confirmar la reserva | Falta registrar el pago (completo o abono) o el abono es menor que el mínimo; el mensaje en amarillo indica qué falta. |
| «No se pudo calcular el total y el pago» | El servidor no respondió la cotización; pulsa **Reintentar**. Si persiste, avisa al administrador. |
| Al refrescar (F5) sale «Not Found» | El sitio desplegado necesita la regla de reescritura `/*` → `/index.html` (ver manual de instalación). |
