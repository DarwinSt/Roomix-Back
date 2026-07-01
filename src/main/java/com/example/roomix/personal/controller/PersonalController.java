package com.example.roomix.personal.controller;

import com.example.roomix.personal.dto.PersonalResponse;
import com.example.roomix.personal.service.PersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Personal", description = "Personal del hotel asignable a incidencias")
@RestController
@RequestMapping("/api/personal")
@RequiredArgsConstructor
public class PersonalController {

    private final PersonalService personalService;

    @Operation(summary = "Listar personal del hotel")
    @GetMapping
    public List<PersonalResponse> listar(
            @RequestParam(required = false) Boolean activo
    ) {
        return personalService.listar(activo);
    }
}
