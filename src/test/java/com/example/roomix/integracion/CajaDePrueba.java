package com.example.roomix.integracion;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Desde Roomix 1.2 la recepción solo registra pagos con un turno de caja abierto a su nombre. Las pruebas que
 * reservan o cobran como recepción abren (o reutilizan) ese turno; el contexto de pruebas es compartido.
 */
final class CajaDePrueba {

    private CajaDePrueba() {
    }

    /** Deja abierto un turno a nombre de {@code usuario}; si la caja la tiene otro, la cierra un administrador. */
    static void abrirPara(MockMvc mvc, String token, String usuario, String tokenAdmin) throws Exception {
        String estado = mvc.perform(get("/api/caja/estado").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map<String, Object> turno = JsonPath.read(estado, "$.turnoAbierto");
        if (turno != null) {
            if (usuario.equalsIgnoreCase(String.valueOf(turno.get("cajero")))) {
                return;
            }
            cerrarComoAdmin(mvc, tokenAdmin);
        }
        mvc.perform(post("/api/caja/abrir").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseInicial\":0,\"observacion\":\"Turno de pruebas automatizadas\"}"))
                .andExpect(status().isCreated());
    }

    /** Cierra el turno abierto (si lo hay) como administrador, con el efectivo esperado y sin base. */
    static void cerrarComoAdmin(MockMvc mvc, String tokenAdmin) throws Exception {
        String estado = mvc.perform(get("/api/caja/estado").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map<String, Object> turno = JsonPath.read(estado, "$.turnoAbierto");
        if (turno == null) {
            return;
        }
        mvc.perform(post("/api/caja/cerrar").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"efectivoContado\":" + turno.get("efectivoEsperado")
                                + ",\"baseSiguiente\":0,\"observacion\":\"Cierre de pruebas automatizadas\"}"))
                .andExpect(status().isOk());
    }
}
