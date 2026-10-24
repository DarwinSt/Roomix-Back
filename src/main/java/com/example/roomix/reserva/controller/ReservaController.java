package com.example.roomix.reserva.controller;

import com.example.roomix.finanzas.dto.FacturaDtos.CuentaResponse;
import com.example.roomix.finanzas.dto.FinanzasDtos.AnularRequest;
import com.example.roomix.finanzas.service.FacturacionService;
import com.example.roomix.habitacion.dto.HabitacionResponse;
import com.example.roomix.reserva.domain.CanalReserva;
import com.example.roomix.reserva.dto.ActualizarReservaRequest;
import com.example.roomix.reserva.service.ReservaOperacionService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.reserva.dto.CrearReservaRequest;
import com.example.roomix.reserva.dto.HabitacionDisponibilidadCheckResponse;
import com.example.roomix.reserva.dto.HabitacionDisponibleResponse;
import com.example.roomix.reserva.dto.ReservaResponse;
import com.example.roomix.reserva.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Reservas", description = "Reservas, disponibilidad e historial de estadías")
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;
    private final ReservaOperacionService operacionService;
    private final FacturacionService facturacionService;

    @Operation(summary = "Listar reservas con filtros opcionales")
    @GetMapping
    public List<ReservaResponse> listar(
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) Long huespedId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta
    ) {
        return reservaService.listar(estado, huespedId, fechaDesde, fechaHasta);
    }

    @Operation(summary = "Obtener reserva por id")
    @GetMapping("/{id}")
    public ReservaResponse obtener(@PathVariable Long id) {
        return reservaService.obtenerPorId(id);
    }

    @Operation(summary = "Consultar habitaciones disponibles para un rango de fechas")
    @GetMapping("/disponibilidad")
    public List<HabitacionDisponibleResponse> disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
            @RequestParam(required = false) TipoHabitacion tipoHabitacion
    ) {
        return reservaService.consultarDisponibilidad(fechaEntrada, fechaSalida, tipoHabitacion);
    }

    @Operation(summary = "Verificar si una habitación concreta está disponible en un rango de fechas")
    @GetMapping("/disponibilidad/habitacion/{habitacionId}")
    public HabitacionDisponibilidadCheckResponse disponibilidadHabitacion(
            @PathVariable Long habitacionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida
    ) {
        return reservaService.verificarDisponibilidadHabitacion(habitacionId, fechaEntrada, fechaSalida);
    }

    @Operation(summary = "Crear reserva y marcar habitación como reservada")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody CrearReservaRequest request) {
        return reservaService.crear(request);
    }

    @Operation(summary = "Modificar reserva",
            description = "Confirmada: fechas, huésped, canal y observaciones. En curso: solo fecha de salida y observaciones")
    @PutMapping("/{id}")
    public ReservaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarReservaRequest request) {
        return reservaService.actualizar(id, request);
    }

    @Operation(summary = "Cancelar reserva confirmada (con motivo)")
    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id, @Valid @RequestBody AnularRequest request) {
        return reservaService.cancelarReserva(id, request.motivo());
    }

    @Operation(summary = "Check-in rápido de la reserva")
    @PatchMapping("/{id}/check-in")
    public HabitacionResponse checkIn(@PathVariable Long id) {
        return operacionService.checkIn(id);
    }

    @Operation(summary = "Check-out de la reserva", description = "Inhabilita la habitación para limpieza y emite la factura")
    @PatchMapping("/{id}/check-out")
    public ReservaOperacionService.CheckOutResponse checkOut(@PathVariable Long id) {
        return operacionService.checkOut(id);
    }

    @Operation(summary = "Cuenta del huésped (alojamiento + servicios, facturado y pagado)")
    @GetMapping("/{id}/cuenta")
    public CuentaResponse cuenta(@PathVariable Long id) {
        return facturacionService.cuentaReserva(id);
    }

    @Operation(summary = "Canales de reserva")
    @GetMapping("/canales")
    public List<java.util.Map<String, String>> canales() {
        return java.util.Arrays.stream(CanalReserva.values())
                .map(c -> java.util.Map.of("codigo", c.name(), "nombre", c.getNombre()))
                .toList();
    }
}
