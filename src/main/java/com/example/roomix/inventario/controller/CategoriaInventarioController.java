package com.example.roomix.inventario.controller;

import com.example.roomix.habitacion.dto.ErrorResponseDoc;
import com.example.roomix.inventario.dto.CategoriaInventarioRequest;
import com.example.roomix.inventario.dto.CategoriaInventarioResponse;
import com.example.roomix.inventario.service.CategoriaInventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Inventario - Categorías",
        description = """
                Categorías del inventario hotelero (Limpieza, Mobiliario, Comida, etc.).
                Incluye descripción y ejemplos de artículos por categoría.
                Puedes crear categorías personalizadas además de las precargadas.
                """
)
@RestController
@RequestMapping("/api/inventario/categorias")
@RequiredArgsConstructor
public class CategoriaInventarioController {

    private final CategoriaInventarioService categoriaInventarioService;

    @Operation(
            summary = "Listar categorías",
            description = "Devuelve las categorías con ejemplos de artículos. Por defecto solo activas."
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = CategoriaInventarioResponse.class))
            )
    )
    @GetMapping
    public List<CategoriaInventarioResponse> listar(
            @Parameter(description = "Si es false, incluye categorías inactivas", example = "true")
            @RequestParam(defaultValue = "true") boolean soloActivas
    ) {
        return categoriaInventarioService.listar(soloActivas);
    }

    @Operation(summary = "Obtener categoría por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CategoriaInventarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "INV-006", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class)))
    })
    @GetMapping("/{id}")
    public CategoriaInventarioResponse obtener(
            @Parameter(example = "1", required = true) @PathVariable Long id
    ) {
        return categoriaInventarioService.obtenerPorId(id);
    }

    @Operation(
            summary = "Crear categoría personalizada",
            description = "Agrega una nueva categoría al inventario (no predefinida)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CategoriaInventarioResponse.class))),
            @ApiResponse(responseCode = "409", description = "INV-007", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaInventarioResponse crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
            @Valid @RequestBody CategoriaInventarioRequest request
    ) {
        return categoriaInventarioService.crear(request);
    }

    @Operation(summary = "Actualizar categoría")
    @PutMapping("/{id}")
    public CategoriaInventarioResponse actualizar(
            @Parameter(example = "8", required = true) @PathVariable Long id,
            @Valid @RequestBody CategoriaInventarioRequest request
    ) {
        return categoriaInventarioService.actualizar(id, request);
    }

    @Operation(
            summary = "Eliminar categoría personalizada",
            description = "Solo categorías no predefinidas y sin artículos asociados (INV-009, INV-010)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Parameter(example = "8", required = true) @PathVariable Long id) {
        categoriaInventarioService.eliminar(id);
    }
}
