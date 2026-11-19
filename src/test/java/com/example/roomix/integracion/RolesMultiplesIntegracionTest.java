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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un usuario con varios roles accede a los módulos de todos ellos; quitar un rol tiene efecto inmediato. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RolesMultiplesIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;

    @Test
    void recepcionistaConRolDeFinanzasAccedeALosDosModulos() throws Exception {
        String admin = token("admin");
        String json = mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"multi.rol","nombreCompleto":"Usuaria Multirrol","email":"multi.rol@roomix.test",
                                 "rol":"RECEPCION","rolesAdicionales":["FINANZAS","RECEPCION"],"password":"%s","activo":true}"""
                                .formatted(CLAVE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles", contains("RECEPCION", "FINANZAS")))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        String multi = token("multi.rol");
        mvc.perform(get("/api/reservas").header("Authorization", "Bearer " + multi)).andExpect(status().isOk());
        mvc.perform(get("/api/finanzas/resumen").header("Authorization", "Bearer " + multi)).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + multi)).andExpect(status().isForbidden());

        // Al quitarle el rol adicional pierde el acceso aunque su token siga vigente
        mvc.perform(put("/api/usuarios/" + id).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"multi.rol","nombreCompleto":"Usuaria Multirrol","email":"multi.rol@roomix.test",
                                 "rol":"RECEPCION","rolesAdicionales":[]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("RECEPCION")));
        mvc.perform(get("/api/finanzas/resumen").header("Authorization", "Bearer " + multi)).andExpect(status().isForbidden());
    }

    private String token(String usuario) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + usuario + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }
}
