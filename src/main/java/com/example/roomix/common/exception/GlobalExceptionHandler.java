package com.example.roomix.common.exception;

import com.example.roomix.habitacion.exception.HabitacionNoDisponibleException;
import com.example.roomix.habitacion.exception.HabitacionNotFoundException;
import com.example.roomix.habitacion.exception.NumeroHabitacionDuplicadoException;
import com.example.roomix.habitacion.exception.TransicionEstadoInvalidaException;
import com.example.roomix.huesped.exception.HuespedException;
import com.example.roomix.inventario.exception.InventarioException;
import com.example.roomix.incidencia.exception.IncidenciaException;
import com.example.roomix.reserva.exception.ReservaException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HuespedException.class)
    public ProblemDetail handleHuesped(HuespedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        detail.setTitle(ex.getHuespedErrorCode().name());
        detail.setProperty("codigo", ex.getErrorCode());
        detail.setProperty("modulo", "HUESPEDES");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(InventarioException.class)
    public ProblemDetail handleInventario(InventarioException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        detail.setTitle(ex.getInventarioErrorCode().name());
        detail.setProperty("codigo", ex.getErrorCode());
        detail.setProperty("modulo", "INVENTARIO");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(IncidenciaException.class)
    public ProblemDetail handleIncidencia(IncidenciaException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        detail.setTitle(ex.getIncidenciaErrorCode().name());
        detail.setProperty("codigo", ex.getErrorCode());
        detail.setProperty("modulo", "INCIDENCIAS");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(ReservaException.class)
    public ProblemDetail handleReserva(ReservaException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        detail.setTitle(ex.getReservaErrorCode().name());
        detail.setProperty("codigo", ex.getErrorCode());
        detail.setProperty("modulo", "RESERVAS");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(HabitacionNoDisponibleException.class)
    public ProblemDetail handleHabitacionNoDisponible(HabitacionNoDisponibleException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        detail.setTitle("Habitación no disponible");
        detail.setProperty("codigo", "HAB-001");
        detail.setProperty("modulo", "HABITACIONES");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(HabitacionNotFoundException.class)
    public ProblemDetail handleHabitacionNotFound(HabitacionNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Habitación no encontrada");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ProblemDetail handleTransicionInvalida(TransicionEstadoInvalidaException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        detail.setTitle("Transición de estado no permitida");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(NumeroHabitacionDuplicadoException.class)
    public ProblemDetail handleNumeroDuplicado(NumeroHabitacionDuplicadoException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Número de habitación duplicado");
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /** Excepciones de negocio de los módulos que no tienen un manejador específico. */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        detail.setTitle(ex.getTitulo());
        detail.setProperty("codigo", ex.getErrorCode());
        if (ex.getModulo() != null) {
            detail.setProperty("modulo", ex.getModulo());
        }
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /** Dos personas guardaron el mismo registro al mismo tiempo: la segunda debe recargar. */
    @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class})
    public ProblemDetail handleVersion(Exception ex) {
        return conflicto("CON-001", "Registro modificado por otra persona",
                "Otra persona modificó este registro al mismo tiempo. Vuelva a cargar la pantalla y repita la operación.");
    }

    /** El registro está bloqueado por otra operación que no terminó a tiempo. */
    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ProblemDetail handleBloqueo(Exception ex) {
        return conflicto("CON-002", "Registro ocupado", "Otra operación está usando este registro. Intente de nuevo en unos segundos.");
    }

    /** Restricciones de la base de datos (por ejemplo, un segundo turno de caja abierto). */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleIntegridad(org.springframework.dao.DataIntegrityViolationException ex) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase();
        if (causa.contains("uk_turno_caja_abierto")) {
            return conflicto("CAJ-OCUPADA", "Caja ocupada", "Ya hay un turno de caja abierto: el hotel tiene una sola caja.");
        }
        return conflicto("CON-003", "Conflicto con otro registro",
                "La operación choca con otro registro (por ejemplo, un dato que ya existe). Revise los datos y vuelva a intentar.");
    }

    private static ProblemDetail conflicto(String codigo, String titulo, String mensaje) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, mensaje);
        detail.setTitle(titulo);
        detail.setProperty("codigo", codigo);
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Error de validación en los datos enviados"
        );
        detail.setTitle("Datos inválidos");
        detail.setProperty("codigo", "VAL-001");
        detail.setProperty("errores", errores);
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
