package com.example.roomix.reserva.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.exception.HabitacionNotFoundException;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import com.example.roomix.reserva.domain.CanalReserva;
import com.example.roomix.reserva.domain.Promocion;
import com.example.roomix.reserva.domain.Promocion.TipoDescuento;
import com.example.roomix.reserva.dto.PromocionDtos.PromocionAplicableResponse;
import com.example.roomix.reserva.dto.PromocionDtos.PromocionRequest;
import com.example.roomix.reserva.dto.PromocionDtos.PromocionResponse;
import com.example.roomix.reserva.exception.ReservaErrorCode;
import com.example.roomix.reserva.exception.ReservaException;
import com.example.roomix.reserva.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Promociones que el administrador configura y que recepción escoge al reservar (una por reserva). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromocionService {

    private final PromocionRepository repository;
    private final HabitacionRepository habitacionRepository;
    private final TarifaService tarifaService;

    public List<PromocionResponse> listar() {
        return repository.findAllByOrderByActivaDescNombreAsc().stream().map(PromocionResponse::from).toList();
    }

    @Transactional
    public PromocionResponse crear(PromocionRequest request) {
        Promocion promocion = new Promocion();
        promocion.setCreadaPor(UsuarioActual.username());
        aplicar(promocion, request);
        return PromocionResponse.from(repository.save(promocion));
    }

    @Transactional
    public PromocionResponse actualizar(Long id, PromocionRequest request) {
        Promocion promocion = buscar(id);
        aplicar(promocion, request);
        return PromocionResponse.from(repository.save(promocion));
    }

    @Transactional
    public PromocionResponse cambiarActiva(Long id, boolean activa) {
        Promocion promocion = buscar(id);
        promocion.setActiva(activa);
        return PromocionResponse.from(repository.save(promocion));
    }

    /** Promociones activas que cumplen las condiciones de la reserva, con el descuento calculado. */
    public List<PromocionAplicableResponse> aplicables(Long habitacionId, LocalDate entrada, LocalDate salida, CanalReserva canal) {
        Habitacion habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new HabitacionNotFoundException(habitacionId));
        int noches = noches(entrada, salida);
        BigDecimal subtotal = tarifa(habitacion).multiply(BigDecimal.valueOf(noches));
        CanalReserva c = canal != null ? canal : CanalReserva.PRESENCIAL;
        return repository.findByActivaTrueOrderByNombreAsc().stream()
                .filter(p -> p.aplica(noches, habitacion.getTipoHabitacion(), c, entrada))
                .map(p -> {
                    BigDecimal descuento = p.descuentoSobre(subtotal);
                    return new PromocionAplicableResponse(p.getId(), p.getNombre(), p.getDescripcion(),
                            PromocionResponse.from(p).condiciones(), p.getTipoDescuento(), p.getValor(), subtotal, descuento,
                            subtotal.subtract(descuento));
                })
                .sorted((a, b) -> b.descuento().compareTo(a.descuento()))
                .toList();
    }

    /** Valida que la promoción aplique y devuelve la entidad; se usa al crear o editar una reserva. */
    public Promocion validarAplicable(Long promocionId, Habitacion habitacion, int noches, CanalReserva canal, LocalDate llegada) {
        Promocion p = buscar(promocionId);
        if (!p.aplica(noches, habitacion.getTipoHabitacion(), canal != null ? canal : CanalReserva.PRESENCIAL, llegada)) {
            throw new ReservaException(ReservaErrorCode.PROMOCION_NO_APLICA,
                    "La promoción «" + p.getNombre() + "» no aplica a esta reserva (" + PromocionResponse.from(p).condiciones() + ")");
        }
        return p;
    }

    private Promocion buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ReservaException(ReservaErrorCode.PROMOCION_NO_ENCONTRADA));
    }

    private void aplicar(Promocion p, PromocionRequest r) {
        if (r.tipoDescuento() == TipoDescuento.PORCENTAJE && r.valor().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ReservaException(ReservaErrorCode.PROMOCION_INVALIDA, "El porcentaje de descuento no puede superar 100");
        }
        if (r.vigenteDesde() != null && r.vigenteHasta() != null && r.vigenteHasta().isBefore(r.vigenteDesde())) {
            throw new ReservaException(ReservaErrorCode.PROMOCION_INVALIDA, "La vigencia termina antes de comenzar");
        }
        p.setNombre(r.nombre().trim());
        p.setDescripcion(StringUtils.hasText(r.descripcion()) ? r.descripcion().trim() : null);
        p.setMinimoNoches(r.minimoNoches());
        p.setTipoHabitacion(r.tipoHabitacion());
        p.setCanal(r.canal());
        p.setVigenteDesde(r.vigenteDesde());
        p.setVigenteHasta(r.vigenteHasta());
        p.setTipoDescuento(r.tipoDescuento());
        p.setValor(r.valor());
        if (r.activa() != null) {
            p.setActiva(r.activa());
        }
    }

    private BigDecimal tarifa(Habitacion h) {
        return h.getPrecioNoche() != null ? h.getPrecioNoche() : tarifaService.obtenerPrecioNoche(h.getTipoHabitacion());
    }

    private static int noches(LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null || !salida.isAfter(entrada)) {
            throw new ReservaException(ReservaErrorCode.FECHAS_INVALIDAS);
        }
        return (int) ChronoUnit.DAYS.between(entrada, salida);
    }
}
