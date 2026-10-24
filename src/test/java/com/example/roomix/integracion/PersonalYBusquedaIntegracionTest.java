package com.example.roomix.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestión del personal (alta, edición, desactivación) y búsqueda rápida de huéspedes para autocompletar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalYBusquedaIntegracionTest {

    @Autowired
    private MockMvc mvc;

    private String token(String usuario) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + usuario + "\",\"password\":\"Roomix2026*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    @Test
    void administradorRegistraEditaYDesactivaPersonal() throws Exception {
        String admin = token("admin");
        String json = mvc.perform(post("/api/personal").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Rosa Elena Pérez\",\"rol\":\"Camarera de pisos\",\"departamento\":\"Housekeeping\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(json, "$.id");

        // Nombre duplicado
        mvc.perform(post("/api/personal").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"rosa elena pérez\",\"rol\":\"Recepcionista\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PER-002"));

        mvc.perform(put("/api/personal/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Rosa Elena Pérez\",\"rol\":\"Supervisora de housekeeping\",\"departamento\":\"Housekeeping\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("Supervisora de housekeeping"));

        mvc.perform(patch("/api/personal/" + id + "/activo").param("valor", "false").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void soloElAdministradorModificaElPersonal() throws Exception {
        mvc.perform(post("/api/personal").header("Authorization", token("recepcion"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Intento\",\"rol\":\"Botones\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void busquedaRapidaPorPalabrasYSinTildes() throws Exception {
        String recepcion = token("recepcion");
        mvc.perform(post("/api/huespedes").header("Authorization", recepcion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Steween","apellidos":"Gómez Mora","tipoDocumento":"CEDULA",
                                 "numeroDocumento":"99887766","email":"steween.gomez@correo.test","telefono":"3001234567",
                                 "autorizaTratamientoDatos":true}
                                """))
                .andExpect(status().isCreated());

        // Nombre y apellido en campos distintos, sin tilde y en minúsculas
        mvc.perform(get("/api/huespedes/buscar").param("q", "steween gomez").header("Authorization", recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].numeroDocumento", hasItem("99887766")));
        // Por documento parcial
        mvc.perform(get("/api/huespedes/buscar").param("q", "998877").header("Authorization", recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].numeroDocumento", hasItem("99887766")));
        // Sin coincidencias
        mvc.perform(get("/api/huespedes/buscar").param("q", "zzz inexistente").header("Authorization", recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
