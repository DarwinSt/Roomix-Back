package com.example.roomix.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.Base64;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configuración por hotel (Roomix 1.2): marca (nombre y logo), IVA opcional, cobro por persona con capacidad solo
 * sugerida y reserva sin pago cuando el hotel no lo exige.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConfiguracionYCobroIntegracionTest {

    private static final String CLAVE = "Roomix2026*";
    /** PNG de 1×1 píxel. */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Autowired
    private MockMvc mvc;

    @AfterEach
    void restablecer() throws Exception {
        // Contexto compartido: se deja el hotel cobrando por habitación, con IVA y con pago obligatorio
        String admin = token("admin");
        enviar(put("/api/configuracion"), admin, """
                {"nombre":"Hotel Roomix","cobraIva":true,"ivaPorcentaje":19,"exentoIvaExtranjeros":true,
                 "modoCobro":"POR_HABITACION","mesesRetencionHuespedes":0}""").andExpect(status().isOk());
        enviar(put("/api/politicas-reserva"), admin, """
                {"abonoMinimoPct":0,"penalidadCancelacionPct":0,"pagoObligatorio":true}""").andExpect(status().isOk());
    }

    @Test
    void marcaDelHotelSeConfiguraYSeVeSinIniciarSesion() throws Exception {
        String admin = token("admin");
        mvc.perform(get("/api/configuracion/publica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modulos").value(hasItem("CAJA")));

        enviar(put("/api/configuracion"), token("recepcion"), """
                {"nombre":"Otro","cobraIva":true,"modoCobro":"POR_HABITACION"}""").andExpect(status().isForbidden());
        enviar(put("/api/configuracion"), admin, """
                {"nombre":"Hotel Mi Kinta","nit":"900.123.456-7","ciudad":"Rivera, Huila","cobraIva":false,
                 "modoCobro":"POR_PERSONA","mesesRetencionHuespedes":4}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Hotel Mi Kinta"))
                .andExpect(jsonPath("$.mesesRetencionHuespedes").value(4))
                .andExpect(jsonPath("$.modulos[?(@.codigo == 'EVENTOS')].habilitado").value(hasItem(true)));

        mvc.perform(multipart("/api/configuracion/logo").file(new MockMultipartFile("archivo", "logo.txt", "text/plain", PNG))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/configuracion/logo").file(new MockMultipartFile("archivo", "logo.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneLogo").value(true));
        mvc.perform(get("/api/configuracion/publica"))
                .andExpect(jsonPath("$.nombre").value("Hotel Mi Kinta"))
                .andExpect(jsonPath("$.tieneLogo").value(true))
                .andExpect(jsonPath("$.modoCobro").value("POR_PERSONA"))
                .andExpect(jsonPath("$.cobraIva").value(false));
        mvc.perform(get("/api/configuracion/logo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        // Los comprobantes y facturas toman el nombre y el IVA de la configuración
        mvc.perform(get("/api/configuracion/hotel").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.nombre").value("Hotel Mi Kinta"))
                .andExpect(jsonPath("$.ivaPorcentaje").value(0));
    }

    @Test
    void cobroPorPersonaConCapacidadSugeridaYReservaSinPago() throws Exception {
        String admin = token("admin");
        enviar(put("/api/configuracion"), admin, """
                {"nombre":"Hotel Roomix","cobraIva":false,"modoCobro":"POR_PERSONA"}""").andExpect(status().isOk());
        long habitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"941","tipoHabitacion":"DOBLE","descripcion":"Cobro por persona","precioNoche":50000,
                 "capacidad":2,"camas":1}""").andExpect(status().isCreated())
                .andExpect(jsonPath("$.camas").value(1)));
        long huespedId = id(enviar(post("/api/huespedes"), admin, """
                {"nombre":"Paula","apellidos":"Por Persona","tipoDocumento":"CEDULA","numeroDocumento":"1047000241",
                 "email":"paula@correo.test","telefono":"+57 300 000 0000","autorizaTratamientoDatos":true}""")
                .andExpect(status().isCreated()));
        LocalDate llegada = LocalDate.now().plusDays(10);

        // 3 personas × 2 noches × $50.000 = $300.000, sin IVA; la capacidad (2) solo genera una advertencia
        mvc.perform(get("/api/reservas/cotizacion").header("Authorization", "Bearer " + admin)
                        .param("habitacionId", String.valueOf(habitacionId))
                        .param("fechaEntrada", llegada.toString()).param("fechaSalida", llegada.plusDays(2).toString())
                        .param("cantidadPersonas", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(300000.0))
                .andExpect(jsonPath("$.impuesto").value(0.0))
                .andExpect(jsonPath("$.cobroPorPersona").value(true))
                .andExpect(jsonPath("$.superaCapacidad").value(true));

        // El hotel permite reservar sin pago
        String sinPago = """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s","canal":"WHATSAPP","cantidadPersonas":3}"""
                .formatted(habitacionId, huespedId, llegada, llegada.plusDays(2));
        enviar(post("/api/reservas"), admin, sinPago).andExpect(status().isUnprocessableEntity());
        enviar(put("/api/politicas-reserva"), admin, """
                {"abonoMinimoPct":0,"penalidadCancelacionPct":0,"pagoObligatorio":false}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagoObligatorio").value(false));
        enviar(post("/api/reservas"), admin, sinPago)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadPersonas").value(3))
                .andExpect(jsonPath("$.cobroPorPersona").value(true))
                .andExpect(jsonPath("$.totalEstimado").value(300000.0))
                .andExpect(jsonPath("$.superaCapacidad").value(true));
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
