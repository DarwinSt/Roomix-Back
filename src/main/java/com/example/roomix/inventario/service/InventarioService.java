package com.example.roomix.inventario.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.common.dto.PaginaResponse;
import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.service.FinanzasService;
import com.example.roomix.inventario.domain.MovimientoInventario;
import com.example.roomix.inventario.domain.TipoMovimientoStock;
import com.example.roomix.inventario.dto.MovimientoInventarioResponse;
import com.example.roomix.inventario.dto.ResumenInventarioResponse;
import com.example.roomix.inventario.repository.MovimientoInventarioRepository;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import com.example.roomix.inventario.domain.ArticuloInventario;
import com.example.roomix.inventario.domain.CategoriaInventario;
import com.example.roomix.inventario.dto.AjustarStockRequest;
import com.example.roomix.inventario.dto.ArticuloInventarioRequest;
import com.example.roomix.inventario.dto.ArticuloInventarioResponse;
import com.example.roomix.inventario.exception.InventarioErrorCode;
import com.example.roomix.inventario.exception.InventarioException;
import com.example.roomix.inventario.repository.ArticuloInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    private final ArticuloInventarioRepository articuloInventarioRepository;
    private final CategoriaInventarioService categoriaInventarioService;
    private final MovimientoInventarioRepository movimientoRepository;
    private final FinanzasService finanzasService;

    public List<ArticuloInventarioResponse> listar(
            Long categoriaId,
            Boolean activo,
            Boolean stockBajo
    ) {
        List<ArticuloInventario> articulos = resolverListado(categoriaId, activo, stockBajo);
        return articulos.stream().map(ArticuloInventarioResponse::from).toList();
    }

    public ArticuloInventarioResponse obtenerPorId(Long id) {
        return ArticuloInventarioResponse.from(buscarConCategoria(id));
    }

    @Transactional
    public ArticuloInventarioResponse crear(ArticuloInventarioRequest request) {
        validarNombreUnico(request.nombre(), null);
        validarCantidadMinima(request.cantidad(), request.cantidadMinima());
        CategoriaInventario categoria = categoriaInventarioService.buscarActiva(request.categoriaId());

        ArticuloInventario articulo = ArticuloInventario.builder()
                .nombre(normalizarNombre(request.nombre()))
                .descripcion(request.descripcion().trim())
                .categoria(categoria)
                .cantidad(request.cantidad())
                .cantidadMinima(request.cantidadMinima())
                .unidadMedida(request.unidadMedida())
                .ubicacion(normalizarUbicacion(request.ubicacion()))
                .proveedor(normalizarUbicacion(request.proveedor()))
                .precioUnitario(request.precioUnitario())
                .activo(request.activo() == null || request.activo())
                .build();

        ArticuloInventario guardado = articuloInventarioRepository.save(articulo);
        if (guardado.getCantidad() > 0) {
            registrarMovimiento(guardado, TipoMovimientoStock.ENTRADA, guardado.getCantidad(), 0,
                    guardado.getCantidad(), guardado.getPrecioUnitario(), "Stock inicial", guardado.getProveedor());
        }
        return ArticuloInventarioResponse.from(guardado);
    }

    @Transactional
    public ArticuloInventarioResponse actualizar(Long id, ArticuloInventarioRequest request) {
        ArticuloInventario articulo = buscarConCategoria(id);
        validarNombreUnico(request.nombre(), id);
        validarCantidadMinima(request.cantidad(), request.cantidadMinima());
        CategoriaInventario categoria = categoriaInventarioService.buscarActiva(request.categoriaId());

        articulo.setNombre(normalizarNombre(request.nombre()));
        articulo.setDescripcion(request.descripcion().trim());
        articulo.setCategoria(categoria);
        int stockAnterior = articulo.getCantidad();
        if (stockAnterior != request.cantidad()) {
            registrarMovimiento(articulo, TipoMovimientoStock.AJUSTE, request.cantidad() - stockAnterior,
                    stockAnterior, request.cantidad(), null, "Ajuste desde la edición del artículo", null);
        }
        articulo.setCantidad(request.cantidad());
        articulo.setCantidadMinima(request.cantidadMinima());
        articulo.setUnidadMedida(request.unidadMedida());
        articulo.setUbicacion(normalizarUbicacion(request.ubicacion()));
        articulo.setProveedor(normalizarUbicacion(request.proveedor()));
        articulo.setPrecioUnitario(request.precioUnitario());
        if (request.activo() != null) {
            articulo.setActivo(request.activo());
        }

        return ArticuloInventarioResponse.from(articuloInventarioRepository.save(articulo));
    }

    @Transactional
    public ArticuloInventarioResponse ajustarStock(Long id, AjustarStockRequest request) {
        ArticuloInventario articulo = buscarActivo(id);
        int anterior = articulo.getCantidad();
        int cantidad = request.cantidad();
        String motivo = request.motivo() != null && !request.motivo().isBlank() ? request.motivo().trim() : null;
        String proveedor = normalizarUbicacion(request.proveedor());

        int nuevo;
        int movido;
        switch (request.tipo()) {
            case ENTRADA -> {
                validarCantidadPositiva(cantidad);
                nuevo = anterior + cantidad;
                movido = cantidad;
            }
            case SALIDA -> {
                validarCantidadPositiva(cantidad);
                if (anterior < cantidad) {
                    throw new InventarioException(
                            InventarioErrorCode.STOCK_INSUFICIENTE, articulo.getNombre(), anterior, cantidad);
                }
                nuevo = anterior - cantidad;
                movido = cantidad;
            }
            default -> {
                // AJUSTE: la cantidad es el conteo físico real
                nuevo = cantidad;
                movido = cantidad - anterior;
                if (motivo == null) {
                    motivo = "Ajuste por conteo físico";
                }
            }
        }

        if (request.tipo() == TipoMovimientoStock.ENTRADA && request.costoUnitario() != null) {
            articulo.setPrecioUnitario(request.costoUnitario());
        }
        if (request.tipo() == TipoMovimientoStock.ENTRADA && proveedor != null) {
            articulo.setProveedor(proveedor);
        }
        articulo.setCantidad(nuevo);
        ArticuloInventario guardado = articuloInventarioRepository.save(articulo);
        registrarMovimiento(guardado, request.tipo(), movido, anterior, nuevo,
                request.tipo() == TipoMovimientoStock.ENTRADA ? request.costoUnitario() : guardado.getPrecioUnitario(),
                motivo, proveedor);

        if (request.tipo() == TipoMovimientoStock.ENTRADA
                && Boolean.TRUE.equals(request.registrarCompra())
                && request.costoUnitario() != null
                && request.costoUnitario().signum() > 0) {
            finanzasService.registrarAutomatico(
                    CategoriaFinanciera.COMPRAS_INVENTARIO,
                    "Compra: " + cantidad + " " + guardado.getUnidadMedida().name().toLowerCase() + " de " + guardado.getNombre(),
                    request.costoUnitario().multiply(BigDecimal.valueOf(cantidad)),
                    null,
                    proveedor != null ? proveedor : guardado.getProveedor(),
                    "INV-" + guardado.getId(),
                    null,
                    null
            );
        }
        return ArticuloInventarioResponse.from(guardado);
    }

    public List<MovimientoInventarioResponse> movimientosDeArticulo(Long articuloId) {
        buscarConCategoria(articuloId);
        return movimientoRepository.deArticulo(articuloId).stream().map(MovimientoInventarioResponse::from).toList();
    }

    public PaginaResponse<MovimientoInventarioResponse> movimientos(
            LocalDate desde, LocalDate hasta, TipoMovimientoStock tipo, int pagina, int tamano) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(30);
        return PaginaResponse.de(
                movimientoRepository.enRango(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(), tipo,
                        PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 200))),
                MovimientoInventarioResponse::from);
    }

    /** Valorización del stock, productos críticos y artículos más usados (últimos N días). */
    public ResumenInventarioResponse resumen(int dias) {
        List<ArticuloInventario> activos = articuloInventarioRepository.findByActivo(true);
        BigDecimal valorTotal = BigDecimal.ZERO;
        Map<String, Object[]> porCategoria = new java.util.TreeMap<>();
        List<ArticuloInventarioResponse> criticos = new ArrayList<>();
        int sinPrecio = 0;
        for (ArticuloInventario a : activos) {
            BigDecimal valor = a.getPrecioUnitario() != null
                    ? a.getPrecioUnitario().multiply(BigDecimal.valueOf(a.getCantidad()))
                    : BigDecimal.ZERO;
            if (a.getPrecioUnitario() == null) {
                sinPrecio++;
            }
            valorTotal = valorTotal.add(valor);
            Object[] acc = porCategoria.computeIfAbsent(a.getCategoria().getNombre(),
                    k -> new Object[]{0L, 0L, BigDecimal.ZERO});
            acc[0] = (Long) acc[0] + 1;
            acc[1] = (Long) acc[1] + a.getCantidad();
            acc[2] = ((BigDecimal) acc[2]).add(valor);
            if (a.getCantidadMinima() != null && a.getCantidad() <= a.getCantidadMinima()) {
                criticos.add(ArticuloInventarioResponse.from(a));
            }
        }
        List<ResumenInventarioResponse.ValorCategoria> categorias = new ArrayList<>();
        porCategoria.forEach((nombre, acc) -> categorias.add(
                new ResumenInventarioResponse.ValorCategoria(nombre, (Long) acc[0], (Long) acc[1], (BigDecimal) acc[2])));

        LocalDateTime hasta = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime desde = hasta.minusDays(Math.max(dias, 1));
        List<ResumenInventarioResponse.ArticuloConsumo> masUsados = movimientoRepository.masUsados(desde, hasta).stream()
                .limit(10)
                .map(f -> {
                    long unidades = ((Number) f[3]).longValue();
                    BigDecimal precio = (BigDecimal) f[4];
                    return new ResumenInventarioResponse.ArticuloConsumo(
                            (Long) f[0], (String) f[1], f[2].toString(), unidades,
                            precio != null ? precio.multiply(BigDecimal.valueOf(unidades)) : null);
                })
                .toList();

        criticos.sort(java.util.Comparator.comparing(ArticuloInventarioResponse::nombre));
        return new ResumenInventarioResponse(activos.size(), valorTotal, sinPrecio, criticos.size(), criticos,
                categorias, dias, masUsados);
    }

    private void registrarMovimiento(ArticuloInventario articulo, TipoMovimientoStock tipo, int cantidad,
                                     int anterior, int resultante, BigDecimal costo, String motivo, String proveedor) {
        movimientoRepository.save(MovimientoInventario.builder()
                .articulo(articulo)
                .tipo(tipo)
                .cantidad(cantidad)
                .stockAnterior(anterior)
                .stockResultante(resultante)
                .costoUnitario(costo)
                .motivo(motivo)
                .proveedor(proveedor)
                .registradoPor(UsuarioActual.username())
                .fechaHora(LocalDateTime.now())
                .build());
    }

    private void validarCantidadPositiva(int cantidad) {
        if (cantidad < 1) {
            throw new InventarioException(InventarioErrorCode.CANTIDAD_MOVIMIENTO_INVALIDA);
        }
    }

    @Transactional
    public void eliminar(Long id) {
        ArticuloInventario articulo = buscarConCategoria(id);
        articulo.setActivo(false);
        articuloInventarioRepository.save(articulo);
    }

    private List<ArticuloInventario> resolverListado(Long categoriaId, Boolean activo, Boolean stockBajo) {
        if (Boolean.TRUE.equals(stockBajo)) {
            return filtrarPorActivo(articuloInventarioRepository.findConStockBajo(), activo);
        }
        if (categoriaId != null) {
            return filtrarPorActivo(articuloInventarioRepository.findByCategoriaId(categoriaId), activo);
        }
        if (activo != null) {
            return articuloInventarioRepository.findByActivo(activo);
        }
        return articuloInventarioRepository.findAllWithCategoria();
    }

    private List<ArticuloInventario> filtrarPorActivo(List<ArticuloInventario> articulos, Boolean activo) {
        if (activo == null) {
            return articulos;
        }
        return articulos.stream().filter(a -> a.isActivo() == activo).toList();
    }

    private ArticuloInventario buscarActivo(Long id) {
        ArticuloInventario articulo = buscarConCategoria(id);
        if (!articulo.isActivo()) {
            throw new InventarioException(InventarioErrorCode.ARTICULO_INACTIVO, articulo.getNombre());
        }
        return articulo;
    }

    private ArticuloInventario buscarConCategoria(Long id) {
        return articuloInventarioRepository.findByIdWithCategoria(id)
                .orElseThrow(() -> new InventarioException(InventarioErrorCode.ARTICULO_NO_ENCONTRADO, id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        String nombreNormalizado = normalizarNombre(nombre);
        if (idExcluido == null) {
            articuloInventarioRepository.findByNombreIgnoreCase(nombreNormalizado)
                    .ifPresent(a -> {
                        throw new InventarioException(InventarioErrorCode.NOMBRE_DUPLICADO, nombreNormalizado);
                    });
            return;
        }
        if (articuloInventarioRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, idExcluido)) {
            throw new InventarioException(InventarioErrorCode.NOMBRE_DUPLICADO, nombreNormalizado);
        }
    }

    private void validarCantidadMinima(Integer cantidad, Integer cantidadMinima) {
        if (cantidadMinima != null && cantidadMinima > cantidad) {
            throw new InventarioException(InventarioErrorCode.CANTIDAD_MINIMA_INVALIDA, cantidad);
        }
    }

    private String normalizarNombre(String nombre) {
        return nombre.trim();
    }

    private String normalizarUbicacion(String ubicacion) {
        if (ubicacion == null || ubicacion.isBlank()) {
            return null;
        }
        return ubicacion.trim();
    }
}
