package com.example.roomix.inventario.controller;

import com.example.roomix.habitacion.dto.ErrorResponseDoc;
import com.example.roomix.inventario.dto.AjustarStockRequest;
import com.example.roomix.inventario.dto.ArticuloInventarioRequest;
import com.example.roomix.inventario.dto.ArticuloInventarioResponse;
import com.example.roomix.inventario.service.InventarioService;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
        name = "Inventario",
        description = """
                Artículos del inventario del hotel. Cada artículo pertenece a una categoría
                (`/api/inventario/categorias`). Incluye fechas de creación/actualización y control de stock.
                Errores: enum `InventarioErrorCode` (INV-001, …).
                """
)
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @Operation(
            summary = "Listar artículos de inventario",
            description = "Filtros opcionales por categoría, estado activo o stock bajo (cantidad ≤ mínimo)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado obtenido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ArticuloInventarioResponse.class))
                    )
            )
    })
    @GetMapping
    public List<ArticuloInventarioResponse> listar(
            @Parameter(description = "Filtrar por ID de categoría", example = "1")
            @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Filtrar por artículos activos/inactivos", example = "true")
            @RequestParam(required = false) Boolean activo,
            @Parameter(description = "Solo artículos con stock en o por debajo del mínimo", example = "true")
            @RequestParam(required = false) Boolean stockBajo
    ) {
        return inventarioService.listar(categoriaId, activo, stockBajo);
    }

    @Operation(summary = "Obtener artículo por ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(schema = @Schema(implementation = ArticuloInventarioResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "INV-001: artículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            )
    })
    @GetMapping("/{id}")
    public ArticuloInventarioResponse obtener(
            @Parameter(description = "ID del artículo", example = "1", required = true)
            @PathVariable Long id
    ) {
        return inventarioService.obtenerPorId(id);
    }

    @Operation(summary = "Registrar artículo en inventario")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    content = @Content(schema = @Schema(implementation = ArticuloInventarioResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o INV-005",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "INV-002: nombre duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticuloInventarioResponse crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ArticuloInventarioRequest.class))
            )
            @Valid @RequestBody ArticuloInventarioRequest request
    ) {
        return inventarioService.crear(request);
    }

    @Operation(summary = "Actualizar artículo de inventario")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(schema = @Schema(implementation = ArticuloInventarioResponse.class))
            ),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class)))
    })
    @PutMapping("/{id}")
    public ArticuloInventarioResponse actualizar(
            @Parameter(description = "ID del artículo", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ArticuloInventarioRequest.class))
            )
            @Valid @RequestBody ArticuloInventarioRequest request
    ) {
        return inventarioService.actualizar(id, request);
    }

    @Operation(
            summary = "Ajustar stock (entrada o salida)",
            description = """
                    Registra un movimiento de inventario. En `SALIDA` valida stock disponible (INV-004 si no alcanza).
                    Solo artículos activos (INV-003 si está inactivo).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(schema = @Schema(implementation = ArticuloInventarioResponse.class))
            ),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "INV-003 o INV-004",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            )
    })
    @PatchMapping("/{id}/stock")
    public ArticuloInventarioResponse ajustarStock(
            @Parameter(description = "ID del artículo", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = AjustarStockRequest.class))
            )
            @Valid @RequestBody AjustarStockRequest request
    ) {
        return inventarioService.ajustarStock(id, request);
    }

    @Operation(
            summary = "Dar de baja artículo (baja lógica)",
            description = "Marca el artículo como inactivo. No elimina el registro de la base de datos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Artículo dado de baja"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @Parameter(description = "ID del artículo", example = "1", required = true)
            @PathVariable Long id
    ) {
        inventarioService.eliminar(id);
    }
}
