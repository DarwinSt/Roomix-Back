package com.example.roomix.config;

import com.example.roomix.config.ConfiguracionDtos.ConfiguracionPublica;
import com.example.roomix.config.ConfiguracionDtos.ConfiguracionRequest;
import com.example.roomix.config.ConfiguracionDtos.ConfiguracionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@Tag(name = "Configuración", description = "Datos, marca y opciones del hotel; módulos de la licencia")
@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionHotelService service;
    private final com.example.roomix.huesped.service.RetencionHuespedesService retencion;

    @Operation(summary = "Datos del hotel para comprobantes y facturas")
    @GetMapping("/hotel")
    public HotelProperties hotel() {
        return service.hotel();
    }

    @Operation(summary = "Nombre, logo, módulos licenciados y opciones de la interfaz (sin autenticación)")
    @GetMapping("/publica")
    public ConfiguracionPublica publica() {
        return service.publica();
    }

    @Operation(summary = "Configuración completa del hotel")
    @GetMapping
    public ConfiguracionResponse detalle() {
        return service.detalle();
    }

    @Operation(summary = "Actualizar datos y opciones del hotel (administrador)")
    @PutMapping
    public ConfiguracionResponse actualizar(@Valid @RequestBody ConfiguracionRequest request) {
        return service.actualizar(request);
    }

    @Operation(summary = "Aplicar ahora la retención de datos de huéspedes (normalmente corre cada noche)")
    @PostMapping("/retencion/ejecutar")
    public java.util.Map<String, Integer> ejecutarRetencion() {
        return java.util.Map.of("anonimizados", retencion.anonimizarInactivos());
    }

    @Operation(summary = "Logo del hotel (sin autenticación, para la pantalla de ingreso)")
    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        Object[] logo = service.logo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType((String) logo[0]))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body((byte[]) logo[1]);
    }

    @Operation(summary = "Subir el logo (PNG, JPG o WebP; máx. 1 MB)")
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConfiguracionResponse subirLogo(@RequestParam("archivo") MultipartFile archivo) {
        return service.guardarLogo(archivo);
    }

    @Operation(summary = "Quitar el logo")
    @DeleteMapping("/logo")
    public ConfiguracionResponse eliminarLogo() {
        return service.eliminarLogo();
    }
}
