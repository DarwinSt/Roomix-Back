package com.example.roomix.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Logo del hotel: forma (original, ícono o avatar) y versión sin fondo con vuelta al original. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoIntegracionTest {

    private static final String CLAVE = "Roomix2026*";
    private static final byte[] ORIGINAL = {1, 2, 3, 4};
    private static final byte[] SIN_FONDO = {9, 8, 7};

    @Autowired
    private MockMvc mvc;

    @Test
    void elLogoSeMuestraConSuFormaYSePuedeVolverAlOriginal() throws Exception {
        String admin = token("admin");
        try {
            subir(admin, ORIGINAL, false).andExpect(jsonPath("$.logoSinFondo").value(false));

            // Forma: la ve también la pantalla de ingreso (configuración pública)
            mvc.perform(put("/api/configuracion/logo/forma").header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"forma\":\"AVATAR\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.logoForma").value("AVATAR"));
            mvc.perform(get("/api/configuracion/publica")).andExpect(jsonPath("$.logoForma").value("AVATAR"));
            mvc.perform(put("/api/configuracion/logo/forma").header("Authorization", "Bearer " + token("recepcion"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"forma\":\"ICONO\"}")).andExpect(status().isForbidden());

            // Versión sin fondo: reemplaza el logo pero guarda el original
            subir(admin, SIN_FONDO, true).andExpect(jsonPath("$.logoSinFondo").value(true));
            assertThat(logo()).containsExactly(SIN_FONDO);
            // Quitar el fondo otra vez no pierde el original
            subir(admin, new byte[]{5, 5}, true);

            mvc.perform(post("/api/configuracion/logo/restaurar").header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.logoSinFondo").value(false));
            assertThat(logo()).containsExactly(ORIGINAL);
            mvc.perform(post("/api/configuracion/logo/restaurar").header("Authorization", "Bearer " + admin))
                    .andExpect(status().isUnprocessableEntity());

            // Un logo nuevo descarta el original anterior
            subir(admin, SIN_FONDO, true);
            subir(admin, new byte[]{7}, false).andExpect(jsonPath("$.logoSinFondo").value(false));
        } finally {
            mvc.perform(delete("/api/configuracion/logo").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
            mvc.perform(put("/api/configuracion/logo/forma").header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"forma\":\"ORIGINAL\"}")).andExpect(status().isOk());
        }
    }

    private org.springframework.test.web.servlet.ResultActions subir(String token, byte[] contenido, boolean sinFondo) throws Exception {
        return mvc.perform(multipart("/api/configuracion/logo").file(new MockMultipartFile("archivo", "logo.png", "image/png", contenido))
                        .param("sinFondo", String.valueOf(sinFondo)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private byte[] logo() throws Exception {
        return mvc.perform(get("/api/configuracion/logo")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
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
