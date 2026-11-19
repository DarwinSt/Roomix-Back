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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Al finalizar una limpieza se registran los materiales gastados y se descuentan del inventario. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialesLimpiezaIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;

    @Test
    void limpiezaDescuentaLosMaterialesDelInventario() throws Exception {
        String admin = token("admin");
        String housekeeping = token("housekeeping");
        Number categoriaId = JsonPath.read(enviar(get("/api/inventario/categorias"), admin, null)
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        long jabon = id(enviar(post("/api/inventario"), admin, """
                {"nombre":"Jabón líquido de prueba","descripcion":"Para baños","categoriaId":%d,"cantidad":10,"cantidadMinima":2,"unidadMedida":"LITRO"}"""
                .formatted(categoriaId.longValue())).andExpect(status().isCreated()));
        long personal = id(enviar(post("/api/personal"), admin, """
                {"nombre":"Marta Limpieza","rol":"Camarera de pisos","departamento":"Housekeeping"}""").andExpect(status().isCreated()));
        String creada = enviar(post("/api/incidencias"), housekeeping, """
                {"alcance":"ZONA_COMUN","ubicacion":"Lobby","tipo":"MANTENIMIENTO","descripcion":"Pintura de muros"}""")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long incidencia = ((Number) JsonPath.read(creada, "$.id")).longValue();
        List<Integer> tareas = JsonPath.read(creada, "$.tareas[*].id");
        enviar(patch("/api/incidencias/" + incidencia + "/asignar"), housekeeping, "{\"personalId\":" + personal + "}")
                .andExpect(status().isOk());
        enviar(patch("/api/incidencias/" + incidencia + "/iniciar"), housekeeping, null).andExpect(status().isOk());
        for (Integer tarea : tareas) {
            enviar(patch("/api/incidencias/" + incidencia + "/tareas/" + tarea), housekeeping, "{\"completada\":true}")
                    .andExpect(status().isOk());
        }

        // Sin stock suficiente no se cierra la tarea ni se descuenta nada
        enviar(patch("/api/incidencias/" + incidencia + "/finalizar"), housekeeping, """
                {"materiales":[{"articuloId":%d,"cantidad":50}]}""".formatted(jabon)).andExpect(status().is4xxClientError());
        enviar(get("/api/inventario/" + jabon), admin, null).andExpect(jsonPath("$.cantidad").value(10));

        // Dos registros del mismo artículo se suman: 3 + 2 = 5 litros
        enviar(patch("/api/incidencias/" + incidencia + "/finalizar"), housekeeping, """
                {"materiales":[{"articuloId":%d,"cantidad":3},{"articuloId":%d,"cantidad":2}]}""".formatted(jabon, jabon))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.materialesUsados[0].articuloNombre").value("Jabón líquido de prueba"))
                .andExpect(jsonPath("$.materialesUsados[0].cantidad").value(5))
                .andExpect(jsonPath("$.materialesUsados[0].unidad").value("LITRO"));
        enviar(get("/api/inventario/" + jabon), admin, null).andExpect(jsonPath("$.cantidad").value(5));
        enviar(get("/api/inventario/" + jabon + "/movimientos"), admin, null)
                .andExpect(jsonPath("$[0].motivo").value("Mantenimiento de Lobby (#" + incidencia + ")"));
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
