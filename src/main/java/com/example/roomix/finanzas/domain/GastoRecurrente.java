package com.example.roomix.finanzas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plantilla de un gasto que se repite cada mes (arriendo, nómina, servicios públicos...). Cada mes genera un egreso
 * pendiente («por pagar») con vencimiento el día indicado, que se confirma cuando se paga.
 */
@Entity
@Table(name = "gastos_recurrentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoRecurrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String concepto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaFinanciera categoria;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    /** Día del mes en que vence (1 a 28, para que exista en todos los meses). */
    @Column(name = "dia_vencimiento", nullable = false)
    private Integer diaVencimiento;

    @Column(length = 150)
    private String tercero;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    /** Último mes generado, con formato AAAA-MM. */
    @Column(name = "ultimo_periodo", length = 7)
    private String ultimoPeriodo;

    @Column(name = "creado_por", length = 50)
    private String creadoPor;

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaHoraCreacion;
}
