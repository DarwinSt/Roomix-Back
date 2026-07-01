package com.example.roomix.reserva.repository;

import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.reserva.domain.Reserva;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class ReservaSpecifications {

    private ReservaSpecifications() {
    }

    public static Specification<Reserva> conFiltros(
            EstadoReserva estado,
            Long huespedId,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    ) {
        Specification<Reserva> spec = Specification.unrestricted();

        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estadoReserva"), estado));
        }
        if (huespedId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("huesped").get("id"), huespedId));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("fechaSalida"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("fechaEntrada"), fechaHasta));
        }

        return spec;
    }
}
