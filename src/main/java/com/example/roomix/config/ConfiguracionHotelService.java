package com.example.roomix.config;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.config.ConfiguracionDtos.ConfiguracionPublica;
import com.example.roomix.config.ConfiguracionDtos.ConfiguracionRequest;
import com.example.roomix.config.ConfiguracionDtos.ConfiguracionResponse;
import com.example.roomix.config.ConfiguracionDtos.ModuloInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;

/** Configuración del hotel guardada en la base de datos (marca, impuestos y opciones) y licencia de módulos. */
@Service
@RequiredArgsConstructor
public class ConfiguracionHotelService {

    private static final Set<String> TIPOS_LOGO = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long MAX_LOGO = 1_048_576;

    private final ConfiguracionHotelRepository repository;
    private final HotelProperties inicial;
    private final Licencia licencia;
    private final com.example.roomix.auth.service.CorreoService correo;

    /**
     * Copia en memoria del registro: se consulta en cada cotización, reserva y factura, y solo cambia por este servicio,
     * así que se lee una vez y se reemplaza al guardar (una sola instancia del backend por hotel).
     */
    private volatile ConfiguracionHotel cache;

    /** Al primer arranque copia las variables HOTEL_* para que una instalación existente conserve sus datos. */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Transactional
    public void inicializar() {
        if (!repository.existsById(ConfiguracionHotel.ID)) {
            repository.save(desdeVariables());
        }
        cache = null;
    }

    @Transactional(readOnly = true)
    public ConfiguracionHotel entidad() {
        ConfiguracionHotel c = cache;
        if (c == null) {
            c = repository.findById(ConfiguracionHotel.ID).orElseGet(this::desdeVariables);
            cache = c;
        }
        return c;
    }

    private ConfiguracionResponse guardar(ConfiguracionHotel c) {
        ConfiguracionHotel guardada = repository.saveAndFlush(c);
        cache = guardada;
        return respuesta(guardada);
    }

    /** Datos del hotel para facturas y cálculos, con el IVA en 0 % si el hotel no lo cobra. */
    @Transactional(readOnly = true)
    public HotelProperties hotel() {
        ConfiguracionHotel c = entidad();
        boolean iva = c.cobraIva();
        return new HotelProperties(c.getNombre(), c.getNit(), c.getDireccion(), c.getCiudad(), c.getTelefono(), c.getEmail(),
                c.getPrefijoFactura(), iva ? (c.getIvaPorcentaje() != null ? c.getIvaPorcentaje() : inicial.ivaPorcentaje()) : BigDecimal.ZERO,
                iva && !Boolean.FALSE.equals(c.getExentoIvaExtranjeros()));
    }

    @Transactional(readOnly = true)
    public boolean cobroPorPersona() {
        return entidad().porPersona();
    }

    @Transactional(readOnly = true)
    public int mesesRetencionHuespedes() {
        Integer m = entidad().getMesesRetencionHuespedes();
        return m == null ? 0 : m;
    }

    @Transactional(readOnly = true)
    public boolean correoConfirmacionReserva() {
        return Boolean.TRUE.equals(entidad().getCorreoConfirmacionReserva());
    }

    @Transactional(readOnly = true)
    public ConfiguracionPublica publica() {
        ConfiguracionHotel c = entidad();
        return new ConfiguracionPublica(c.getNombre(), c.tieneLogo(), version(c),
                licencia.habilitados().stream().sorted().toList(), modo(c), c.cobraIva(),
                c.getColorPrimario(), c.getColorAcento(), c.getEslogan(), c.forma());
    }

    @Transactional(readOnly = true)
    public ConfiguracionResponse detalle() {
        return respuesta(entidad());
    }

    @Transactional
    public ConfiguracionResponse actualizar(ConfiguracionRequest r) {
        ConfiguracionHotel c = repository.findById(ConfiguracionHotel.ID).orElseGet(this::desdeVariables);
        c.setNombre(r.nombre().trim());
        c.setNit(texto(r.nit()));
        c.setDireccion(texto(r.direccion()));
        c.setCiudad(texto(r.ciudad()));
        c.setTelefono(texto(r.telefono()));
        c.setEmail(texto(r.email()));
        c.setPrefijoFactura(StringUtils.hasText(r.prefijoFactura()) ? r.prefijoFactura().trim().toUpperCase() : "FV");
        c.setCobraIva(r.cobraIva());
        if (r.ivaPorcentaje() != null) {
            c.setIvaPorcentaje(r.ivaPorcentaje().setScale(2, RoundingMode.HALF_UP));
        }
        if (r.exentoIvaExtranjeros() != null) {
            c.setExentoIvaExtranjeros(r.exentoIvaExtranjeros());
        }
        c.setModoCobro(r.modoCobro());
        c.setMesesRetencionHuespedes(r.mesesRetencionHuespedes() == null ? 0 : r.mesesRetencionHuespedes());
        c.setCorreoConfirmacionReserva(Boolean.TRUE.equals(r.correoConfirmacionReserva()));
        c.setActualizadaPor(UsuarioActual.username());
        return guardar(c);
    }

    @Transactional
    public ConfiguracionResponse guardarLogo(MultipartFile archivo) {
        return guardarLogo(archivo, false);
    }

    /**
     * Guarda el logo. Si es la versión sin fondo del logo actual, se conserva el original (la primera vez) para poder
     * volver a él; un logo nuevo reemplaza también al original.
     */
    @Transactional
    public ConfiguracionResponse guardarLogo(MultipartFile archivo, boolean sinFondo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAX_LOGO
                || archivo.getContentType() == null || !TIPOS_LOGO.contains(archivo.getContentType().toLowerCase())) {
            throw new ConfiguracionException(ConfiguracionErrorCode.LOGO_INVALIDO);
        }
        ConfiguracionHotel c = repository.findById(ConfiguracionHotel.ID).orElseGet(this::desdeVariables);
        if (sinFondo && c.tieneLogo()) {
            if (c.getLogoOriginal() == null) {
                c.setLogoOriginal(c.getLogo());
                c.setLogoOriginalTipo(c.getLogoTipo());
            }
        } else {
            c.setLogoOriginal(null);
            c.setLogoOriginalTipo(null);
        }
        try {
            c.setLogo(archivo.getBytes());
        } catch (IOException e) {
            throw new ConfiguracionException(ConfiguracionErrorCode.LOGO_INVALIDO);
        }
        c.setLogoTipo(archivo.getContentType().toLowerCase());
        c.setActualizadaPor(UsuarioActual.username());
        return guardar(c);
    }

    @Transactional
    public ConfiguracionResponse eliminarLogo() {
        ConfiguracionHotel c = repository.findById(ConfiguracionHotel.ID).orElseGet(this::desdeVariables);
        c.setLogo(null);
        c.setLogoTipo(null);
        c.setLogoOriginal(null);
        c.setLogoOriginalTipo(null);
        c.setActualizadaPor(UsuarioActual.username());
        return guardar(c);
    }

    /** Vuelve al logo como se subió (deshace «quitar el fondo»). */
    @Transactional
    public ConfiguracionResponse restaurarLogo() {
        ConfiguracionHotel c = repository.findById(ConfiguracionHotel.ID).orElseGet(this::desdeVariables);
        if (c.getLogoOriginal() == null) {
            throw new ConfiguracionException(ConfiguracionErrorCode.SIN_LOGO_ORIGINAL);
        }
        c.setLogo(c.getLogoOriginal());
        c.setLogoTipo(c.getLogoOriginalTipo());
        c.setLogoOriginal(null);
        c.setLogoOriginalTipo(null);
        c.setActualizadaPor(UsuarioActual.username());
        return guardar(c);
    }

    @Transactional
    public ConfiguracionResponse cambiarFormaLogo(LogoForma forma) {
        ConfiguracionHotel c = repository.findById(ConfiguracionHotel.ID).orElseGet(this::desdeVariables);
        c.setLogoForma(forma);
        c.setActualizadaPor(UsuarioActual.username());
        return guardar(c);
    }

    /** Colores de la interfaz y frase de bienvenida; en blanco vuelve a los de Roomix. */
    @Transactional
    public ConfiguracionResponse actualizarApariencia(ConfiguracionDtos.AparienciaRequest r) {
        ConfiguracionHotel c = repository.findById(ConfiguracionHotel.ID).orElseGet(this::desdeVariables);
        c.setColorPrimario(StringUtils.hasText(r.colorPrimario()) ? r.colorPrimario().toLowerCase() : null);
        c.setColorAcento(StringUtils.hasText(r.colorAcento()) ? r.colorAcento().toLowerCase() : null);
        c.setEslogan(texto(r.eslogan()));
        c.setActualizadaPor(UsuarioActual.username());
        return guardar(c);
    }

    /** Logo del hotel: [tipo, contenido]. */
    @Transactional(readOnly = true)
    public Object[] logo() {
        ConfiguracionHotel c = entidad();
        if (!c.tieneLogo() || c.getLogo() == null) {
            throw new ConfiguracionException(ConfiguracionErrorCode.SIN_LOGO);
        }
        return new Object[]{c.getLogoTipo(), c.getLogo()};
    }

    private ConfiguracionResponse respuesta(ConfiguracionHotel c) {
        HotelProperties h = hotel();
        return new ConfiguracionResponse(c.getNombre(), c.getNit(), c.getDireccion(), c.getCiudad(), c.getTelefono(), c.getEmail(),
                c.getPrefijoFactura(), c.cobraIva(), c.getIvaPorcentaje() != null ? c.getIvaPorcentaje() : inicial.ivaPorcentaje(),
                !Boolean.FALSE.equals(c.getExentoIvaExtranjeros()), modo(c),
                c.getMesesRetencionHuespedes() == null ? 0 : c.getMesesRetencionHuespedes(),
                Boolean.TRUE.equals(c.getCorreoConfirmacionReserva()), correo.habilitado(), c.tieneLogo(), version(c), licencia.cliente(),
                Arrays.stream(Modulo.values()).map(m -> new ModuloInfo(m, m.getNombre(), licencia.habilitado(m))).toList(),
                c.getColorPrimario(), c.getColorAcento(), c.getEslogan(), c.forma(), c.getLogoOriginal() != null);
    }

    private ConfiguracionHotel desdeVariables() {
        return ConfiguracionHotel.builder()
                .id(ConfiguracionHotel.ID)
                .nombre(inicial.nombre())
                .nit(inicial.nit())
                .direccion(inicial.direccion())
                .ciudad(inicial.ciudad())
                .telefono(inicial.telefono())
                .email(inicial.email())
                .prefijoFactura(inicial.prefijoFactura())
                .cobraIva(inicial.ivaPorcentaje().signum() > 0)
                .ivaPorcentaje(inicial.ivaPorcentaje())
                .exentoIvaExtranjeros(inicial.exentoIvaExtranjeros())
                .modoCobro(ModoCobro.POR_HABITACION)
                .mesesRetencionHuespedes(0)
                .correoConfirmacionReserva(false)
                .build();
    }

    private static ModoCobro modo(ConfiguracionHotel c) {
        return c.getModoCobro() != null ? c.getModoCobro() : ModoCobro.POR_HABITACION;
    }

    private static long version(ConfiguracionHotel c) {
        return c.getFechaHoraActualizacion() == null ? 0
                : c.getFechaHoraActualizacion().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static String texto(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
