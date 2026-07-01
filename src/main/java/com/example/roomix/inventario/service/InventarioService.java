package com.example.roomix.inventario.service;

import com.example.roomix.inventario.domain.ArticuloInventario;
import com.example.roomix.inventario.domain.CategoriaInventario;
import com.example.roomix.inventario.domain.TipoMovimientoStock;
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
                .activo(request.activo() == null || request.activo())
                .build();

        return ArticuloInventarioResponse.from(articuloInventarioRepository.save(articulo));
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
        articulo.setCantidad(request.cantidad());
        articulo.setCantidadMinima(request.cantidadMinima());
        articulo.setUnidadMedida(request.unidadMedida());
        articulo.setUbicacion(normalizarUbicacion(request.ubicacion()));
        if (request.activo() != null) {
            articulo.setActivo(request.activo());
        }

        return ArticuloInventarioResponse.from(articuloInventarioRepository.save(articulo));
    }

    @Transactional
    public ArticuloInventarioResponse ajustarStock(Long id, AjustarStockRequest request) {
        ArticuloInventario articulo = buscarActivo(id);

        int cantidadMovimiento = request.cantidad();
        if (request.tipo() == TipoMovimientoStock.SALIDA) {
            if (articulo.getCantidad() < cantidadMovimiento) {
                throw new InventarioException(
                        InventarioErrorCode.STOCK_INSUFICIENTE,
                        articulo.getNombre(),
                        articulo.getCantidad(),
                        cantidadMovimiento
                );
            }
            articulo.setCantidad(articulo.getCantidad() - cantidadMovimiento);
        } else {
            articulo.setCantidad(articulo.getCantidad() + cantidadMovimiento);
        }

        return ArticuloInventarioResponse.from(articuloInventarioRepository.save(articulo));
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
