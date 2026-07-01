package com.example.roomix.inventario.service;

import com.example.roomix.inventario.domain.CategoriaInventario;
import com.example.roomix.inventario.dto.CategoriaInventarioRequest;
import com.example.roomix.inventario.dto.CategoriaInventarioResponse;
import com.example.roomix.inventario.exception.InventarioErrorCode;
import com.example.roomix.inventario.exception.InventarioException;
import com.example.roomix.inventario.repository.ArticuloInventarioRepository;
import com.example.roomix.inventario.repository.CategoriaInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaInventarioService {

    private final CategoriaInventarioRepository categoriaInventarioRepository;
    private final ArticuloInventarioRepository articuloInventarioRepository;

    public List<CategoriaInventarioResponse> listar(Boolean soloActivas) {
        List<CategoriaInventario> categorias = Boolean.TRUE.equals(soloActivas)
                ? categoriaInventarioRepository.findByActivoTrueOrderByNombreAsc()
                : categoriaInventarioRepository.findAllByOrderByNombreAsc();
        return categorias.stream().map(CategoriaInventarioResponse::from).toList();
    }

    public CategoriaInventarioResponse obtenerPorId(Long id) {
        return CategoriaInventarioResponse.from(buscar(id));
    }

    @Transactional
    public CategoriaInventarioResponse crear(CategoriaInventarioRequest request) {
        validarNombreUnico(request.nombre(), null);

        CategoriaInventario categoria = CategoriaInventario.builder()
                .nombre(normalizar(request.nombre()))
                .descripcion(request.descripcion().trim())
                .ejemplosArticulos(request.ejemplosArticulos().trim())
                .activo(request.activo() == null || request.activo())
                .predefinida(false)
                .build();

        return CategoriaInventarioResponse.from(categoriaInventarioRepository.save(categoria));
    }

    @Transactional
    public CategoriaInventarioResponse actualizar(Long id, CategoriaInventarioRequest request) {
        CategoriaInventario categoria = buscar(id);
        validarNombreUnico(request.nombre(), id);

        categoria.setNombre(normalizar(request.nombre()));
        categoria.setDescripcion(request.descripcion().trim());
        categoria.setEjemplosArticulos(request.ejemplosArticulos().trim());
        if (request.activo() != null) {
            categoria.setActivo(request.activo());
        }

        return CategoriaInventarioResponse.from(categoriaInventarioRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        CategoriaInventario categoria = buscar(id);

        if (categoria.isPredefinida()) {
            throw new InventarioException(InventarioErrorCode.CATEGORIA_PREDEFINIDA, categoria.getNombre());
        }

        long totalArticulos = articuloInventarioRepository.countByCategoriaId(id);
        if (totalArticulos > 0) {
            throw new InventarioException(
                    InventarioErrorCode.CATEGORIA_CON_ARTICULOS,
                    categoria.getNombre(),
                    totalArticulos
            );
        }

        categoriaInventarioRepository.delete(categoria);
    }

    CategoriaInventario buscarActiva(Long id) {
        CategoriaInventario categoria = buscar(id);
        if (!categoria.isActivo()) {
            throw new InventarioException(InventarioErrorCode.CATEGORIA_INACTIVA, categoria.getNombre());
        }
        return categoria;
    }

    private CategoriaInventario buscar(Long id) {
        return categoriaInventarioRepository.findById(id)
                .orElseThrow(() -> new InventarioException(InventarioErrorCode.CATEGORIA_NO_ENCONTRADA, id));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        String normalizado = normalizar(nombre);
        if (idExcluido == null) {
            categoriaInventarioRepository.findByNombreIgnoreCase(normalizado)
                    .ifPresent(c -> {
                        throw new InventarioException(InventarioErrorCode.CATEGORIA_NOMBRE_DUPLICADO, normalizado);
                    });
            return;
        }
        if (categoriaInventarioRepository.existsByNombreIgnoreCaseAndIdNot(normalizado, idExcluido)) {
            throw new InventarioException(InventarioErrorCode.CATEGORIA_NOMBRE_DUPLICADO, normalizado);
        }
    }

    private String normalizar(String nombre) {
        return nombre.trim();
    }
}
