package com.example.roomix.integracion;

import com.example.roomix.auth.service.CorreoService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Correos de la reserva: confirmación, cambio y cancelación, con su estado guardado y reenvío manual. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorreoReservaEstadoIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CorreoService correo;

    @AfterEach
    void restablecer() throws Exception {
        enviar(put("/api/configuracion"), token("admin"), """
                {"nombre":"Hotel Roomix","cobraIva":true,"ivaPorcentaje":19,"modoCobro":"POR_HABITACION",
                 "mesesRetencionHuespedes":0,"correoConfirmacionReserva":false}""").andExpect(status().isOk());
    }

    @Test
    void elHuespedRecibeConfirmacionCambioYCancelacionYSeVeElEstado() throws Exception {
        when(correo.habilitado()).thenReturn(true);
        when(correo.enviarHtml(any(), any(), any(), any(), any(), any())).thenReturn(true);
        String admin = token("admin");
        enviar(put("/api/configuracion"), admin, """
                {"nombre":"Hotel Mi Kinta","cobraIva":true,"modoCobro":"POR_HABITACION","correoConfirmacionReserva":true}""")
                .andExpect(status().isOk());
        long habitacion = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"981","tipoHabitacion":"DOBLE","descripcion":"Correos de reserva","precioNoche":120000}""")
                .andExpect(status().isCreated()));
        long huesped = id(enviar(post("/api/huespedes"), admin, """
                {"nombre":"Marta","apellidos":"Correo Prueba","tipoDocumento":"CEDULA","numeroDocumento":"1047000981",
                 "email":"marta@correo.test","telefono":"+57 300 000 0000","autorizaTratamientoDatos":true}""")
                .andExpect(status().isCreated()));
        LocalDate llegada = LocalDate.now().plusDays(30);
        long reserva = id(enviar(post("/api/reservas"), admin, """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s","canal":"WHATSAPP",
                 "abonoInicial":{"valor":50000,"metodo":"TRANSFERENCIA"}}""".formatted(habitacion, huesped, llegada, llegada.plusDays(2)))
                .andExpect(status().isCreated()));

        verify(correo, timeout(5000)).enviarHtml(eq("marta@correo.test"), any(), eq("Hotel Mi Kinta"),
                eq("Reserva confirmada #" + reserva + " — Hotel Mi Kinta"), contains("confirmada"), any());
        esperarEstado(admin, reserva, "ENVIADO", "CONFIRMACION");

        // Cambio de fechas: se avisa con los datos nuevos
        enviar(put("/api/reservas/" + reserva), admin, """
                {"fechaEntrada":"%s","fechaSalida":"%s"}""".formatted(llegada, llegada.plusDays(3))).andExpect(status().isOk());
        verify(correo, timeout(5000)).enviarHtml(eq("marta@correo.test"), any(), any(),
                eq("Reserva modificada #" + reserva + " — Hotel Mi Kinta"), contains("datos nuevos"), any());
        esperarEstado(admin, reserva, "ENVIADO", "MODIFICACION");

        // Cancelación
        enviar(patch("/api/reservas/" + reserva + "/cancelar"), admin, "{\"motivo\":\"El huésped cambió de planes\"}")
                .andExpect(status().isOk());
        verify(correo, timeout(5000)).enviarHtml(eq("marta@correo.test"), any(), any(),
                eq("Reserva cancelada #" + reserva + " — Hotel Mi Kinta"), contains("cancelada"), any());
        esperarEstado(admin, reserva, "ENVIADO", "CANCELACION");

        // Si el envío falla, queda registrado; el reenvío manual devuelve el estado
        when(correo.enviarHtml(any(), any(), any(), any(), any(), any())).thenReturn(false);
        enviar(post("/api/reservas/" + reserva + "/correo"), token("recepcion"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correoEstado").value("FALLIDO"))
                .andExpect(jsonPath("$.correoTipo").value("CANCELACION"));

        // Sin correo configurado en el servidor no se puede reenviar
        when(correo.habilitado()).thenReturn(false);
        enviar(post("/api/reservas/" + reserva + "/correo"), admin, null).andExpect(status().isUnprocessableEntity());
    }

    private void esperarEstado(String token, long reserva, String estado, String tipo) throws Exception {
        String json = "";
        for (int i = 0; i < 50; i++) {
            json = enviar(get("/api/reservas/" + reserva), token, null).andReturn().getResponse().getContentAsString();
            if (estado.equals(JsonPath.read(json, "$.correoEstado")) && tipo.equals(JsonPath.read(json, "$.correoTipo"))) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat((String) JsonPath.read(json, "$.correoTipo")).isEqualTo(tipo);
        assertThat((String) JsonPath.read(json, "$.correoEstado")).isEqualTo(estado);
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
