package com.example.roomix.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Abono mínimo para reservar y abonos visibles en la reserva, promociones configurables por el administrador,
 * devolución con penalidad al cancelar y gastos recurrentes por pagar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AbonosPromocionesGastosIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @AfterEach
    void restablecerPoliticas() throws Exception {
        // El contexto de pruebas es compartido: se deja la política sin abono mínimo para las demás pruebas
        enviar(put("/api/politicas-reserva"), token("admin"), """
                {"abonoMinimoPct":0,"penalidadCancelacionPct":0}""").andExpect(status().isOk());
    }

    @Test
    void reservaConPromocionYAbonoMinimoSeFacturaConSusAbonos() throws Exception {
        String admin = token("admin");
        String recepcion = token("recepcion");
        String finanzas = token("finanzas");
        LocalDate llegada = LocalDate.now();

        // Políticas: 50 % de abono para reservar y 20 % de penalidad al cancelar (solo el administrador las cambia)
        enviar(put("/api/politicas-reserva"), recepcion, """
                {"abonoMinimoPct":50,"penalidadCancelacionPct":20}""").andExpect(status().isForbidden());
        enviar(put("/api/politicas-reserva"), admin, """
                {"abonoMinimoPct":50,"penalidadCancelacionPct":20}""").andExpect(status().isOk())
                .andExpect(jsonPath("$.abonoMinimoPct").value(50.0));

        // Promoción del administrador: más de 5 noches → $30.000 de descuento
        enviar(post("/api/promociones"), admin, """
                {"nombre":"Estadía larga","minimoNoches":6,"tipoDescuento":"VALOR_FIJO","valor":30000}""")
                .andExpect(status().isCreated());

        long habitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"921","tipoHabitacion":"DOBLE","descripcion":"Habitación de abonos","precioNoche":100000}""")
                .andExpect(status().isCreated()));
        long huespedId = huesped(recepcion, "Diana", "1047000221");

        // Aplicables: con 6 noches aparece la promoción; con 3 no
        mvc.perform(get("/api/promociones/aplicables").header("Authorization", "Bearer " + recepcion)
                        .param("habitacionId", String.valueOf(habitacionId)).param("fechaEntrada", llegada.toString())
                        .param("fechaSalida", llegada.plusDays(3).toString()))
                .andExpect(jsonPath("$[?(@.nombre == 'Estadía larga')]").isEmpty());
        String aplicables = mvc.perform(get("/api/promociones/aplicables").header("Authorization", "Bearer " + recepcion)
                        .param("habitacionId", String.valueOf(habitacionId)).param("fechaEntrada", llegada.toString())
                        .param("fechaSalida", llegada.plusDays(6).toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(aplicables, "$[?(@.nombre == 'Estadía larga')].id");
        List<Double> descuentos = JsonPath.read(aplicables, "$[?(@.nombre == 'Estadía larga')].descuento");
        assertThat(descuentos).containsExactly(30000.0);
        long promocionId = ids.getFirst();

        // Total: 6 × 100.000 − 30.000 = 570.000; con IVA 19 % = 678.300; abono mínimo 50 % = 339.150
        String sinAbono = """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s","promocionId":%d}"""
                .formatted(habitacionId, huespedId, llegada, llegada.plusDays(6), promocionId);
        enviar(post("/api/reservas"), recepcion, sinAbono)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RES-015"));
        // El cambio de estado de la habitación no crea reservas (se saltaría el pago)
        enviar(patch("/api/habitaciones/" + habitacionId + "/estado"), admin, """
                {"estado":"RESERVADO","fechaEntrada":"%s","fechaSalida":"%s","huespedId":%d}"""
                .formatted(llegada, llegada.plusDays(2), huespedId))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RES-016"));
        enviar(post("/api/reservas"), recepcion, sinAbono.replace("}", ",\"abonoInicial\":{\"valor\":300000,\"metodo\":\"EFECTIVO\"}}"))
                .andExpect(status().isUnprocessableEntity());
        long reservaId = id(enviar(post("/api/reservas"), recepcion,
                sinAbono.replace("}", ",\"abonoInicial\":{\"valor\":339150,\"metodo\":\"TRANSFERENCIA\",\"referencia\":\"TR-1\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(600000.0))
                .andExpect(jsonPath("$.descuento").value(30000.0))
                .andExpect(jsonPath("$.totalEstimado").value(570000.0))
                .andExpect(jsonPath("$.promocionNombre").value("Estadía larga")));

        // Abonos visibles en la reserva; un segundo abono y el rechazo de uno que supera el saldo
        mvc.perform(get("/api/reservas/" + reservaId + "/abonos").header("Authorization", "Bearer " + recepcion))
                .andExpect(jsonPath("$.totalReserva").value(678300.0))
                .andExpect(jsonPath("$.abonoMinimo").value(339150.0))
                .andExpect(jsonPath("$.totalAbonado").value(339150.0))
                .andExpect(jsonPath("$.saldo").value(339150.0))
                .andExpect(jsonPath("$.abonos[0].metodo").value("TRANSFERENCIA"));
        enviar(post("/api/reservas/" + reservaId + "/abonos"), recepcion, """
                {"valor":100000,"metodo":"EFECTIVO"}""").andExpect(status().isCreated());
        enviar(post("/api/reservas/" + reservaId + "/abonos"), recepcion, """
                {"valor":500000,"metodo":"EFECTIVO"}""").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("FIN-012"));

        // Check-in y check-out: la factura toma los abonos como pagos y solo queda el saldo
        enviar(patch("/api/reservas/" + reservaId + "/check-in"), recepcion, null).andExpect(status().isOk());
        String checkout = enviar(patch("/api/reservas/" + reservaId + "/check-out"), recepcion, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factura.total").value(678300.0))
                .andExpect(jsonPath("$.factura.totalPagado").value(439150.0))
                .andExpect(jsonPath("$.factura.estado").value("PAGADA_PARCIAL"))
                .andReturn().getResponse().getContentAsString();
        String numero = JsonPath.read(checkout, "$.factura.numero");
        // Los abonos ya se registraron como ingreso al recibirse: la factura no los vuelve a registrar
        mvc.perform(get("/api/finanzas/movimientos").param("texto", "RES-" + reservaId).header("Authorization", "Bearer " + finanzas))
                .andExpect(jsonPath("$.totalElementos").value(2));
        mvc.perform(get("/api/finanzas/movimientos").param("texto", numero).header("Authorization", "Bearer " + finanzas))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void cancelarConAbonoRetieneLaPenalidadYDevuelveElResto() throws Exception {
        String admin = token("admin");
        String recepcion = token("recepcion");
        String finanzas = token("finanzas");
        enviar(put("/api/politicas-reserva"), admin, """
                {"abonoMinimoPct":50,"penalidadCancelacionPct":20}""").andExpect(status().isOk());
        long habitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"922","tipoHabitacion":"INDIVIDUAL","descripcion":"Habitación de cancelación","precioNoche":100000}""")
                .andExpect(status().isCreated()));
        long huespedId = huesped(recepcion, "Elena", "1047000222");
        LocalDate llegada = LocalDate.now().plusDays(10);
        // 2 noches = 200.000 + IVA = 238.000; abono 119.000 (50 %)
        long reservaId = id(enviar(post("/api/reservas"), recepcion, """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s",
                 "abonoInicial":{"valor":119000,"metodo":"EFECTIVO"}}""".formatted(habitacionId, huespedId, llegada, llegada.plusDays(2)))
                .andExpect(status().isCreated()));

        enviar(patch("/api/reservas/" + reservaId + "/cancelar"), recepcion, """
                {"motivo":"El huésped canceló el viaje"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorPenalidad").value(23800.0))
                .andExpect(jsonPath("$.valorDevuelto").value(95200.0));
        mvc.perform(get("/api/reservas/" + reservaId + "/abonos").header("Authorization", "Bearer " + recepcion))
                .andExpect(jsonPath("$.abonos[0].devuelto").value(true))
                .andExpect(jsonPath("$.abonos[0].valorDevuelto").value(95200.0));
        mvc.perform(get("/api/finanzas/movimientos").param("texto", "RES-" + reservaId).param("tipo", "EGRESO")
                        .header("Authorization", "Bearer " + finanzas))
                .andExpect(jsonPath("$.contenido[0].valor").value(95200.0));
    }

    @Test
    void gastoRecurrenteQuedaPorPagarHastaConfirmarElPago() throws Exception {
        String finanzas = token("finanzas");
        LocalDate hoy = LocalDate.now();
        String resumen0 = mvc.perform(get("/api/finanzas/resumen").header("Authorization", "Bearer " + finanzas))
                .andReturn().getResponse().getContentAsString();
        double egresos0 = ((Number) JsonPath.read(resumen0, "$.totalEgresos")).doubleValue();
        double porPagar0 = ((Number) JsonPath.read(resumen0, "$.porPagar")).doubleValue();

        int dia = Math.min(hoy.getDayOfMonth(), 28);
        String creado = enviar(post("/api/finanzas/gastos-recurrentes"), finanzas, """
                {"concepto":"Arriendo del local","categoria":"ARRIENDO","valor":2400000,"diaVencimiento":%d,"tercero":"Inmobiliaria QA"}"""
                .formatted(dia))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaNombre").exists())
                .andReturn().getResponse().getContentAsString();
        // Al editar el gasto, el pendiente del mes (aún sin pagar) toma el valor nuevo y no se duplica
        enviar(put("/api/finanzas/gastos-recurrentes/" + JsonPath.read(creado, "$.id")), finanzas, """
                {"concepto":"Arriendo del local","categoria":"ARRIENDO","valor":2500000,"diaVencimiento":%d,"tercero":"Inmobiliaria QA"}"""
                .formatted(dia))
                .andExpect(status().isOk());
        enviar(post("/api/finanzas/gastos-recurrentes"), finanzas, """
                {"concepto":"Ingreso mal clasificado","categoria":"ALOJAMIENTO","valor":1000,"diaVencimiento":5}""")
                .andExpect(status().isBadRequest());

        String pendientes = mvc.perform(get("/api/finanzas/movimientos").param("pendiente", "true").param("texto", "Arriendo del local")
                        .header("Authorization", "Bearer " + finanzas))
                .andExpect(jsonPath("$.contenido[0].pendiente").value(true))
                .andExpect(jsonPath("$.contenido[0].valor").value(2500000.0))
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        int movimientoId = JsonPath.read(pendientes, "$.contenido[0].id");

        // Pendiente: aparece en «por pagar» y no en los egresos del periodo
        mvc.perform(get("/api/finanzas/resumen").header("Authorization", "Bearer " + finanzas))
                .andExpect(jsonPath("$.porPagar").value(porPagar0 + 2500000))
                .andExpect(jsonPath("$.totalEgresos").value(egresos0));

        enviar(patch("/api/finanzas/movimientos/" + movimientoId + "/pagar"), finanzas, """
                {"metodoPago":"TRANSFERENCIA","referencia":"PSE-123"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendiente").value(false));
        mvc.perform(get("/api/finanzas/resumen").header("Authorization", "Bearer " + finanzas))
                .andExpect(jsonPath("$.porPagar").value(porPagar0))
                .andExpect(jsonPath("$.totalEgresos").value(egresos0 + 2500000))
                .andExpect(jsonPath("$.egresosPorCategoria[*].categoria", hasItem("ARRIENDO")));
        // Un gasto ya pagado no se vuelve a pagar
        enviar(patch("/api/finanzas/movimientos/" + movimientoId + "/pagar"), finanzas, "{}")
                .andExpect(status().isUnprocessableEntity());
    }

    private long huesped(String token, String nombre, String documento) throws Exception {
        return id(enviar(post("/api/huespedes"), token, """
                {"nombre":"%s","apellidos":"Prueba Abonos","tipoDocumento":"CEDULA","numeroDocumento":"%s",
                 "email":"%s@correo.test","telefono":"+57 300 000 0000","autorizaTratamientoDatos":true}"""
                .formatted(nombre, documento, documento)).andExpect(status().isCreated()));
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
