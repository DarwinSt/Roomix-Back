package com.example.roomix.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Separación de responsabilidades: quien cobra no anula, y las anulaciones quedan para revisión con quién y por qué. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnulacionesIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;

    @Test
    void recepcionCobraPeroNoAnulaYLaAdministracionVeLasAnulaciones() throws Exception {
        String admin = token("admin");
        String recepcion = token("recepcion");

        // Los permisos por defecto: Recepción no anula; Finanzas sí
        enviar(get("/api/usuarios/permisos"), admin, null)
                .andExpect(jsonPath("$.porRol.RECEPCION").value(not(hasItem("ANULACIONES"))))
                .andExpect(jsonPath("$.porRol.FINANZAS").value(hasItem("ANULACIONES")));

        Number categoriaId = JsonPath.read(enviar(get("/api/inventario/categorias"), admin, null)
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        long agua = id(enviar(post("/api/inventario"), admin, """
                {"nombre":"Agua de prueba anulaciones","descripcion":"Minibar","categoriaId":%d,"cantidad":20,"unidadMedida":"UNIDAD",
                 "precioVenta":3000}""".formatted(categoriaId.longValue())).andExpect(status().isCreated()));
        long venta = id(enviar(post("/api/ventas"), admin, """
                {"modo":"CONTADO","items":[{"articuloId":%d,"cantidad":2}],"metodoPago":"EFECTIVO"}""".formatted(agua))
                .andExpect(status().isCreated()));

        // Recepción vende, pero anular es de otra persona
        enviar(patch("/api/ventas/" + venta + "/anular"), recepcion, "{\"motivo\":\"Me equivoqué\"}").andExpect(status().isForbidden());
        enviar(patch("/api/facturas/999999/anular"), recepcion, "{\"motivo\":\"Prueba\"}").andExpect(status().isForbidden());
        enviar(get("/api/auditoria/anulaciones"), recepcion, null).andExpect(status().isForbidden());

        enviar(patch("/api/ventas/" + venta + "/anular"), admin, "{\"motivo\":\"El huésped devolvió las botellas\"}")
                .andExpect(status().isOk());
        enviar(get("/api/auditoria/anulaciones"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipo=='VENTA' && @.id==%d)].motivo".formatted(venta)).value(hasItem("El huésped devolvió las botellas")))
                .andExpect(jsonPath("$[?(@.tipo=='VENTA' && @.id==%d)].anuladoPor".formatted(venta)).value(hasItem("admin")))
                .andExpect(jsonPath("$[?(@.tipo=='VENTA' && @.id==%d)].mismaPersona".formatted(venta)).value(hasItem(true)))
                // El ingreso de la venta no se repite como movimiento anulado
                .andExpect(jsonPath("$[*].referencia").value(not(hasItem("Venta de productos"))));
    }

    private ResultActions enviar(MockHttpServletRequestBuilder builder, String token, String cuerpo) throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (cuerpo != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(builder);
    }

    private long id(ResultActions acciones) throws Exception {
        Number id = JsonPath.read(acciones.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
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
