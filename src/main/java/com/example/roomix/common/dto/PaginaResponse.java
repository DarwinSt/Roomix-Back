package com.example.roomix.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página de resultados con metadatos de paginación. */
public record PaginaResponse<T>(
        List<T> contenido,
        long totalElementos,
        int totalPaginas,
        int pagina,
        int tamano
) {
    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
