package com.example.roomix.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un hotel con licencia solo de caja e inventario no accede a servicios, eventos ni reportes. */
@SpringBootTest(properties = {"app.licencia.modulos=CAJA, inventario", "app.licencia.cliente=Hotel de prueba"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LicenciaIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void losModulosNoLicenciadosResponden403() throws Exception {
        String admin = token();
        mvc.perform(get("/api/configuracion/publica"))
                .andExpect(jsonPath("$.modulos", contains("CAJA", "INVENTARIO")));
        for (String ruta : new String[]{"/api/servicios/catalogo", "/api/eventos", "/api/salones", "/api/reportes/ocupacion"}) {
            mvc.perform(get(ruta).header("Authorization", "Bearer " + admin))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/caja/estado").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        mvc.perform(get("/api/inventario").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        mvc.perform(get("/api/configuracion").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.clienteLicencia").value("Hotel de prueba"));
    }

    private String token() throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Roomix2026*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }
}
