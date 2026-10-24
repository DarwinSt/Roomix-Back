package com.example.roomix.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad sobre la aplicación completa: autenticación JWT y permisos por rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadIntegracionTest {

    @Autowired
    private MockMvc mvc;

    String token(String usuario) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + usuario + "\",\"password\":\"Roomix2026*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/habitaciones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-401"));
    }

    @Test
    void tokenAlteradoResponde401() throws Exception {
        String token = token("admin");
        mvc.perform(get("/api/habitaciones").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void credencialesIncorrectasResponden401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("AUT-001"));
    }

    @Test
    void healthYSwaggerSonPublicos() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api-docs")).andExpect(status().isOk());
    }

    @Test
    void perfilDelUsuarioAutenticado() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token("housekeeping")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("HOUSEKEEPING"));
    }

    @Test
    void cadaRolSoloAccedeASusModulos() throws Exception {
        String housekeeping = "Bearer " + token("housekeeping");
        String finanzas = "Bearer " + token("finanzas");
        String almacen = "Bearer " + token("almacen");
        String recepcion = "Bearer " + token("recepcion");

        mvc.perform(get("/api/incidencias").header("Authorization", housekeeping)).andExpect(status().isOk());
        mvc.perform(get("/api/reservas").header("Authorization", housekeeping)).andExpect(status().isForbidden());
        mvc.perform(get("/api/finanzas/resumen").header("Authorization", finanzas)).andExpect(status().isOk());
        mvc.perform(get("/api/finanzas/resumen").header("Authorization", recepcion)).andExpect(status().isForbidden());
        mvc.perform(get("/api/inventario").header("Authorization", almacen)).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").header("Authorization", almacen))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("AUT-403"));
    }

    @Test
    void corsPermiteElFrontendYLaAppAndroid() throws Exception {
        mvc.perform(options("/api/habitaciones")
                        .header("Origin", "https://localhost")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://localhost"));
        mvc.perform(options("/api/habitaciones")
                        .header("Origin", "https://sitio-malicioso.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
