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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Permisos por casillas: el rol marca sus permisos por defecto y el administrador agrega o quita módulos a cada
 * usuario; el cambio aplica de inmediato.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermisosIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;

    @Test
    void elRolMarcaLosPermisosYElAdministradorLosAjusta() throws Exception {
        String admin = token("admin");
        mvc.perform(get("/api/usuarios/permisos").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porRol.RECEPCION").value(hasItem("RESERVAS_GESTIONAR")))
                .andExpect(jsonPath("$.porRol.RECEPCION").value(not(hasItem("FINANZAS"))));

        // Recepcionista que además ve Finanzas, pero sin acceso a eventos
        String json = mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"recep.finanzas","nombreCompleto":"Recepción con finanzas","email":"recep.finanzas@roomix.test",
                                 "rol":"RECEPCION","password":"%s","activo":true,
                                 "permisos":["HABITACIONES_VER","HABITACIONES_OPERAR","RESERVAS_VER","RESERVAS_GESTIONAR","FACTURAS","FINANZAS"]}"""
                                .formatted(CLAVE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permisosPersonalizados").value(true))
                .andExpect(jsonPath("$.permisos").value(hasItem("FINANZAS")))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        String usuario = token("recep.finanzas");
        mvc.perform(get("/api/reservas").header("Authorization", "Bearer " + usuario)).andExpect(status().isOk());
        mvc.perform(get("/api/finanzas/resumen").header("Authorization", "Bearer " + usuario)).andExpect(status().isOk());
        mvc.perform(get("/api/eventos").header("Authorization", "Bearer " + usuario)).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + usuario)).andExpect(status().isForbidden());

        // Volver a la plantilla del rol: se omiten los permisos; pierde Finanzas y recupera Eventos de inmediato
        mvc.perform(put("/api/usuarios/" + id).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"recep.finanzas","nombreCompleto":"Recepción con finanzas","email":"recep.finanzas@roomix.test",
                                 "rol":"RECEPCION"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permisosPersonalizados").value(false));
        mvc.perform(get("/api/finanzas/resumen").header("Authorization", "Bearer " + usuario)).andExpect(status().isForbidden());
        mvc.perform(get("/api/eventos").header("Authorization", "Bearer " + usuario)).andExpect(status().isOk());

        // El último usuario con permiso de usuarios no puede perderlo
        String admins = mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        Number adminId = JsonPath.<java.util.List<Number>>read(admins, "$[?(@.username == 'admin')].id").get(0);
        mvc.perform(put("/api/usuarios/" + adminId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"admin","nombreCompleto":"Administrador","email":"admin@roomix.test","rol":"ADMIN",
                                 "permisos":["RESERVAS_VER"]}"""))
                .andExpect(status().is4xxClientError());
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
