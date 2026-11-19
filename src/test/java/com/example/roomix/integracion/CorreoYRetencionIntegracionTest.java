package com.example.roomix.integracion;

import com.example.roomix.auth.service.CorreoService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Correo de confirmación al reservar (si el hotel lo activa) y anonimización de huéspedes inactivos. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorreoYRetencionIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CorreoService correo;

    @AfterEach
    void restablecer() throws Exception {
        enviar(put("/api/configuracion"), token("admin"), """
                {"nombre":"Hotel Roomix","cobraIva":true,"ivaPorcentaje":19,"modoCobro":"POR_HABITACION",
                 "mesesRetencionHuespedes":0,"correoConfirmacionReserva":false}""").andExpect(status().isOk());
    }

    @Test
    void alReservarSeEnviaLaConfirmacionSoloSiElHotelLaActivo() throws Exception {
        when(correo.habilitado()).thenReturn(true);
        String admin = token("admin");
        long habitacion = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"961","tipoHabitacion":"DOBLE","descripcion":"Correo de confirmación","precioNoche":120000}""")
                .andExpect(status().isCreated()));
        long huesped = huesped(admin, "Lucía", "1047000261", "lucia@correo.test");
        LocalDate llegada = LocalDate.now().plusDays(20);
        String reserva = """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s","canal":"WHATSAPP",
                 "abonoInicial":{"valor":50000,"metodo":"TRANSFERENCIA"}}""";

        // Desactivado: no se envía
        enviar(post("/api/reservas"), admin, reserva.formatted(habitacion, huesped, llegada, llegada.plusDays(1)))
                .andExpect(status().isCreated());
        verify(correo, timeout(1500).times(0)).enviarHtml(any(), any(), any(), any(), any(), any());

        enviar(put("/api/configuracion"), admin, """
                {"nombre":"Hotel Mi Kinta","cobraIva":true,"modoCobro":"POR_HABITACION","correoConfirmacionReserva":true}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.correoDisponible").value(true));
        long id = id(enviar(post("/api/reservas"), admin, reserva.formatted(habitacion, huesped, llegada.plusDays(3), llegada.plusDays(5)))
                .andExpect(status().isCreated()));
        verify(correo, timeout(5000)).enviarHtml(eq("lucia@correo.test"), contains("Lucía"), eq("Hotel Mi Kinta"),
                eq("Reserva confirmada #" + id + " — Hotel Mi Kinta"), contains("Saldo por pagar"), contains("Abonado: $"));
    }

    @Test
    void losHuespedesInactivosSeAnonimizanConservandoSusReservas() throws Exception {
        String admin = token("admin");
        long antiguo = huesped(admin, "Andrés", "1047000262", "andres@correo.test");
        long reciente = huesped(admin, "Beatriz", "1047000263", "beatriz@correo.test");
        jdbc.update("UPDATE huespedes SET fecha_hora_creacion = ? WHERE id = ?", LocalDateTime.now().minusMonths(6), antiguo);

        // Sin plazo configurado no se anonimiza a nadie
        enviar(post("/api/configuracion/retencion/ejecutar"), admin, null).andExpect(jsonPath("$.anonimizados").value(0));
        enviar(put("/api/configuracion"), admin, """
                {"nombre":"Hotel Roomix","cobraIva":true,"modoCobro":"POR_HABITACION","mesesRetencionHuespedes":4}""")
                .andExpect(status().isOk());
        enviar(post("/api/configuracion/retencion/ejecutar"), token("recepcion"), null).andExpect(status().isForbidden());
        enviar(post("/api/configuracion/retencion/ejecutar"), admin, null).andExpect(jsonPath("$.anonimizados").value(1));

        enviar(get("/api/huespedes/" + antiguo), admin, null)
                .andExpect(jsonPath("$.nombre").value("Huésped"))
                .andExpect(jsonPath("$.numeroDocumento").value("ANON-" + antiguo))
                .andExpect(jsonPath("$.email").value("anonimizado-" + antiguo + "@sin-datos.invalid"));
        enviar(get("/api/huespedes/" + reciente), admin, null).andExpect(jsonPath("$.nombre").value("Beatriz"));
        verify(correo, never()).enviarHtml(eq("andres@correo.test"), any(), any(), any(), any(), any());
    }

    private long huesped(String token, String nombre, String documento, String email) throws Exception {
        return id(enviar(post("/api/huespedes"), token, """
                {"nombre":"%s","apellidos":"Prueba","tipoDocumento":"CEDULA","numeroDocumento":"%s","email":"%s",
                 "telefono":"+57 300 000 0000","autorizaTratamientoDatos":true}""".formatted(nombre, documento, email))
                .andExpect(status().isCreated()));
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
