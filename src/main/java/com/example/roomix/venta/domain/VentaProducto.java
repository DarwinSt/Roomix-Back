package com.example.roomix.venta.domain;

import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.reserva.domain.Reserva;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Venta de productos del inventario (minibar, tienda, aseo) a un huésped o a un cliente de paso. */
@Entity
@Table(name = "ventas_productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModoVenta modo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.VIGENTE;

    /** Estadía a la que se carga (solo en CARGO_HABITACION). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    /** Nombre del cliente (opcional en ventas de contado). */
    @Column(length = 150)
    private String cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 30)
    private MetodoPago metodoPago;

    @Column(length = 60)
    private String referencia;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    /** Ingreso en Finanzas (y en la caja del turno) de una venta de contado. */
    @Column(name = "movimiento_financiero_id")
    private Long movimientoFinancieroId;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @Builder.Default
    private List<VentaProductoItem> items = new ArrayList<>();

    @Column(name = "registrada_por", nullable = false, length = 80)
    private String registradaPor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "anulada_por", length = 80)
    private String anuladaPor;

    @Column(name = "fecha_hora_anulacion")
    private LocalDateTime fechaHoraAnulacion;

    public boolean vigente() {
        return estado == EstadoVenta.VIGENTE;
    }
}
