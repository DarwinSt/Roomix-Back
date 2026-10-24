package com.example.roomix.habitacion.service;

import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.domain.HabitacionFoto;
import com.example.roomix.habitacion.exception.HabitacionNotFoundException;
import com.example.roomix.habitacion.repository.HabitacionFotoRepository;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fotografías de habitaciones (máximo 6 por habitación, 2 MB cada una, JPG/PNG/WebP).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitacionFotoService {

    static final int MAX_FOTOS = 6;
    static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final HabitacionFotoRepository fotoRepository;
    private final HabitacionRepository habitacionRepository;

    /** Ids de fotos por habitación, en una sola consulta y sin cargar las imágenes. */
    public Map<Long, List<Long>> idsPorHabitacion(Collection<Long> habitacionIds) {
        Map<Long, List<Long>> mapa = new HashMap<>();
        if (habitacionIds.isEmpty()) {
            return mapa;
        }
        for (Object[] fila : fotoRepository.idsPorHabitacion(habitacionIds)) {
            mapa.computeIfAbsent((Long) fila[0], k -> new ArrayList<>()).add((Long) fila[1]);
        }
        return mapa;
    }

    public HabitacionFoto obtener(Long habitacionId, Long fotoId) {
        return fotoRepository.findByIdAndHabitacionId(fotoId, habitacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto no encontrada"));
    }

    @Transactional
    public List<Long> subir(Long habitacionId, MultipartFile archivo) {
        Habitacion habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new HabitacionNotFoundException(habitacionId));
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione una imagen");
        }
        if (archivo.getContentType() == null || !TIPOS.contains(archivo.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no permitido: use JPG, PNG o WebP");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen supera 2 MB");
        }
        long actuales = fotoRepository.countByHabitacionId(habitacionId);
        if (actuales >= MAX_FOTOS) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La habitación ya tiene el máximo de " + MAX_FOTOS + " fotos");
        }
        try {
            fotoRepository.save(HabitacionFoto.builder()
                    .habitacion(habitacion)
                    .contenido(archivo.getBytes())
                    .tipoContenido(archivo.getContentType())
                    .nombreArchivo(archivo.getOriginalFilename())
                    .tamanoBytes((int) archivo.getSize())
                    .orden((int) actuales + 1)
                    .fechaHoraCreacion(LocalDateTime.now())
                    .build());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen");
        }
        return idsPorHabitacion(List.of(habitacionId)).getOrDefault(habitacionId, List.of());
    }

    @Transactional
    public List<Long> eliminar(Long habitacionId, Long fotoId) {
        fotoRepository.delete(obtener(habitacionId, fotoId));
        return idsPorHabitacion(List.of(habitacionId)).getOrDefault(habitacionId, List.of());
    }
}
