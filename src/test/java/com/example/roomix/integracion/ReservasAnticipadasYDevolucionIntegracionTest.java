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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión de los hallazgos de las pruebas funcionales:
 * H-01 reservas anticipadas de una habitación ocupada (validación por cruce de fechas) y
 * H-02 devolución de los pagos al anular una factura.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservasAnticipadasYDevolucionIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void habitacionOcupadaAceptaReservaFuturaSinCruceYLaActivaAlSalir() throws Exception {
        String admin = token("admin");
        String recepcion = token("recepcion");
        CajaDePrueba.abrirPara(mvc, recepcion, "recepcion", token("admin"));
        LocalDate hoy = LocalDate.now();

        long habitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"911","tipoHabitacion":"DOBLE","descripcion":"Habitación H-01","precioNoche":200000}""")
                .andExpect(status().isCreated()));
        long otraHabitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"912","tipoHabitacion":"DOBLE","descripcion":"Habitación H-01 bis","precioNoche":200000}""")
                .andExpect(status().isCreated()));
        long ana = huesped(recepcion, "Ana", "1047000211");
        long bruno = huesped(recepcion, "Bruno", "1047000212");

        long actual = id(reservar(recepcion, habitacionId, ana, hoy, hoy.plusDays(2)).andExpect(status().isCreated()));
        enviar(patch("/api/reservas/" + actual + "/check-in"), recepcion, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OCUPADO"));

        // H-01: la habitación está ocupada hoy, pero acepta una reserva futura que no se cruza
        long futura = id(reservar(recepcion, habitacionId, bruno, hoy.plusDays(5), hoy.plusDays(7))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA")));
        // ...y sigue rechazando las que se cruzan con la estadía en curso
        reservar(recepcion, habitacionId, bruno, hoy.plusDays(1), hoy.plusDays(3))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RES-003"));
        // El mismo huésped no puede tener dos reservas que se cruzan, aunque sean en habitaciones distintas
        reservar(recepcion, otraHabitacionId, bruno, hoy.plusDays(6), hoy.plusDays(8))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RES-011"));
        // La habitación sigue mostrando la estadía en curso
        mvc.perform(get("/api/habitaciones/" + habitacionId).header("Authorization", "Bearer " + recepcion))
                .andExpect(jsonPath("$.estado").value("OCUPADO"))
                .andExpect(jsonPath("$.fechaEntrada").value(hoy.toString()));

        // Al hacer el check-out, la habitación pasa a limpieza y queda asociada a la próxima reserva
        enviar(patch("/api/reservas/" + actual + "/check-out"), recepcion, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habitacion.estado").value("INHABILITADO"))
                .andExpect(jsonPath("$.habitacion.fechaEntrada").value(hoy.plusDays(5).toString()));
        mvc.perform(get("/api/reservas/" + futura).header("Authorization", "Bearer " + recepcion))
                .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"));
    }

    @Test
    void anularFacturaPagadaDevuelveLosPagos() throws Exception {
        String admin = token("admin");
        String recepcion = token("recepcion");
        CajaDePrueba.abrirPara(mvc, recepcion, "recepcion", token("admin"));
        String finanzas = token("finanzas");
        LocalDate hoy = LocalDate.now();

        long habitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"913","tipoHabitacion":"INDIVIDUAL","descripcion":"Habitación H-02","precioNoche":150000}""")
                .andExpect(status().isCreated()));
        long carla = huesped(recepcion, "Carla", "1047000213");
        long reservaId = id(reservar(recepcion, habitacionId, carla, hoy, hoy.plusDays(1)).andExpect(status().isCreated()));
        enviar(patch("/api/reservas/" + reservaId + "/check-in"), recepcion, null).andExpect(status().isOk());
        String checkout = enviar(patch("/api/reservas/" + reservaId + "/check-out"), recepcion, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int facturaId = JsonPath.read(checkout, "$.factura.id");
        String numero = JsonPath.read(checkout, "$.factura.numero");
        double saldo = ((Number) JsonPath.read(checkout, "$.factura.saldo")).doubleValue();

        enviar(post("/api/facturas/" + facturaId + "/pagos"), recepcion, """
                {"valor":%s,"metodo":"EFECTIVO"}""".formatted(saldo))
                .andExpect(jsonPath("$.estado").value("PAGADA"));

        // H-02: la anulación de una factura pagada devuelve el pago
        enviar(patch("/api/facturas/" + facturaId + "/anular"), finanzas, """
                {"motivo":"Error en los datos del cliente"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                // Una factura anulada no queda con saldo por cobrar
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.pagos[0].devuelto").value(true))
                .andExpect(jsonPath("$.pagos[1].devuelto").value(true));
        // El ingreso original se conserva y se registra el egreso de devolución por el mismo valor
        mvc.perform(get("/api/finanzas/movimientos").param("texto", numero).header("Authorization", "Bearer " + finanzas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].tipo", hasItem("INGRESO")))
                .andExpect(jsonPath("$.contenido[?(@.tipo == 'EGRESO')].valor", hasItem(saldo)))
                .andExpect(jsonPath("$.contenido[?(@.tipo == 'EGRESO')].valor", hasItem(10000.0)));
    }

    private long huesped(String token, String nombre, String documento) throws Exception {
        return id(enviar(post("/api/huespedes"), token, """
                {"nombre":"%s","apellidos":"Prueba Regresión","tipoDocumento":"CEDULA","numeroDocumento":"%s",
                 "email":"%s@correo.test","telefono":"+57 300 000 0000","autorizaTratamientoDatos":true}"""
                .formatted(nombre, documento, documento)).andExpect(status().isCreated()));
    }

    private ResultActions reservar(String token, long habitacionId, long huespedId, LocalDate entrada, LocalDate salida) throws Exception {
        return enviar(post("/api/reservas"), token, """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s","canal":"TELEFONO",
                 "abonoInicial":{"valor":10000,"metodo":"EFECTIVO"}}"""
                .formatted(habitacionId, huespedId, entrada, salida));
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
