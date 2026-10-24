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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de una estadía a través de la API (integración frontend–backend–base de datos):
 * habitación → huésped → reserva → check-in → servicio → check-out con factura → pago → ingresos → auditoría.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlujoHoteleroIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void estadiaCompletaConFacturacionYTrazabilidad() throws Exception {
        String admin = token("admin");
        String recepcion = token("recepcion");
        String eventos = token("eventos");
        String finanzas = token("finanzas");
        LocalDate hoy = LocalDate.now();

        long habitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"901","tipoHabitacion":"DOBLE","descripcion":"Habitación de prueba","precioNoche":250000,
                 "caracteristicas":["WiFi"]}""").andExpect(status().isCreated())
                .andExpect(jsonPath("$.piso").value(9))
                .andExpect(jsonPath("$.capacidad").value(2)));

        long huespedId = id(enviar(post("/api/huespedes"), recepcion, """
                {"nombre":"Laura","apellidos":"Gómez Ríos","tipoDocumento":"CEDULA","numeroDocumento":"1047000111",
                 "email":"laura.gomez@correo.test","telefono":"+57 300 111 2233","nacionalidad":"Colombia",
                 "autorizaTratamientoDatos":true}""").andExpect(status().isCreated()));

        long reservaId = id(enviar(post("/api/reservas"), recepcion, """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s","canal":"WHATSAPP"}"""
                .formatted(habitacionId, huespedId, hoy, hoy.plusDays(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalEstimado").value(500000.0))
                .andExpect(jsonPath("$.canal").value("WHATSAPP")));

        // Doble asignación: la misma habitación no se puede reservar en fechas que se cruzan
        enviar(post("/api/reservas"), recepcion, """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s"}"""
                .formatted(habitacionId, huespedId, hoy.plusDays(1), hoy.plusDays(3)))
                .andExpect(status().isUnprocessableEntity());

        enviar(patch("/api/reservas/" + reservaId + "/check-in"), recepcion, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OCUPADO"));

        long servicioId = id(enviar(post("/api/servicios/catalogo"), eventos, """
                {"nombre":"Lavandería express prueba","categoria":"LAVANDERIA","precio":45000,"unidad":"por bolsa"}""")
                .andExpect(status().isCreated()));
        enviar(post("/api/servicios/solicitudes"), recepcion, """
                {"reservaId":%d,"servicioId":%d,"cantidad":2}""".formatted(reservaId, servicioId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(90000.0));

        // Check-out: habitación a limpieza + factura automática (alojamiento + servicio + IVA 19 %)
        String checkout = enviar(patch("/api/reservas/" + reservaId + "/check-out"), recepcion, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habitacion.estado").value("INHABILITADO"))
                .andExpect(jsonPath("$.factura.subtotal").value(590000.0))
                .andExpect(jsonPath("$.factura.impuesto").value(112100.0))
                .andExpect(jsonPath("$.factura.total").value(702100.0))
                .andReturn().getResponse().getContentAsString();
        int facturaId = JsonPath.read(checkout, "$.factura.id");
        String numero = JsonPath.read(checkout, "$.factura.numero");
        assertThat(numero).startsWith("FV-");

        enviar(post("/api/facturas/" + facturaId + "/pagos"), recepcion, """
                {"valor":702100,"metodo":"TARJETA_CREDITO","referencia":"123456"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"));

        // El pago se reparte en ingresos por categoría en el módulo financiero
        mvc.perform(get("/api/finanzas/movimientos").param("texto", numero).header("Authorization", "Bearer " + finanzas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[*].categoria", hasItem("ALOJAMIENTO")))
                .andExpect(jsonPath("$.contenido[*].categoria", hasItem("SERVICIOS")));

        // Trazabilidad: la auditoría registra quién hizo el check-out
        mvc.perform(get("/api/auditoria").param("accion", "CHECK_OUT").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].usuario").value("recepcion"))
                .andExpect(jsonPath("$.contenido[0].entidadId").value(String.valueOf(reservaId)));
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
                        .content("{\"username\":\"" + usuario + "\",\"password\":\"Roomix2026*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }
}
