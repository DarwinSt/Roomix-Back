package com.example.roomix.habitacion.controller;

import com.example.roomix.habitacion.domain.HabitacionFoto;
import com.example.roomix.habitacion.service.HabitacionFotoService;
import com.example.roomix.reserva.dto.ReservaResponse;
import com.example.roomix.reserva.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@Tag(name = "Habitaciones", description = "Fotografías e historial de ocupación")
@RestController
@RequestMapping("/api/habitaciones/{habitacionId}")
@RequiredArgsConstructor
public class HabitacionFotoController {

    private final HabitacionFotoService fotoService;
    private final ReservaService reservaService;

    @Operation(summary = "Ver foto (público para mostrarla con <img>)")
    @GetMapping("/fotos/{fotoId}")
    public ResponseEntity<byte[]> foto(@PathVariable Long habitacionId, @PathVariable Long fotoId) {
        HabitacionFoto foto = fotoService.obtener(habitacionId, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipoContenido()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(foto.getContenido());
    }

    @Operation(summary = "Subir foto (JPG, PNG o WebP; máx. 2 MB; 6 por habitación)")
    @PostMapping(value = "/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Long> subir(@PathVariable Long habitacionId, @RequestParam("archivo") MultipartFile archivo) {
        return fotoService.subir(habitacionId, archivo);
    }

    @Operation(summary = "Eliminar foto")
    @DeleteMapping("/fotos/{fotoId}")
    public List<Long> eliminar(@PathVariable Long habitacionId, @PathVariable Long fotoId) {
        return fotoService.eliminar(habitacionId, fotoId);
    }

    @Operation(summary = "Historial de ocupación (reservas de la habitación, más recientes primero)")
    @GetMapping("/historial")
    public List<ReservaResponse> historial(@PathVariable Long habitacionId) {
        return reservaService.historialHabitacion(habitacionId);
    }
}
