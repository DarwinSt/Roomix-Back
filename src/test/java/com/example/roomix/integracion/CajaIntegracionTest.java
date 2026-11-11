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
 * Caja de la recepción por turnos: sin turno no se cobra; los abonos y movimientos del cajero entran solos al turno;
 * retiros y aportes de efectivo; arqueo con faltante explicado; base para el siguiente turno; entrega entre cajeros.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CajaIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;

    @Test
    void turnoCompletoConArqueoYEntregaAlSiguienteCajero() throws Exception {
        String admin = token("admin");
        String recepcion = token("recepcion");
        String finanzas = token("finanzas");
        CajaDePrueba.cerrarComoAdmin(mvc, admin);

        enviar(post("/api/usuarios"), admin, """
                {"username":"cajera.noche","nombreCompleto":"Cajera Turno Noche","email":"cajera.noche@roomix.test",
                 "rol":"RECEPCION","password":"%s","activo":true}""".formatted(CLAVE)).andExpect(status().isCreated());
        String cajera = token("cajera.noche");

        long habitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"931","tipoHabitacion":"DOBLE","descripcion":"Habitación de caja","precioNoche":100000}""")
                .andExpect(status().isCreated()));
        long huespedId = id(enviar(post("/api/huespedes"), recepcion, """
                {"nombre":"Gloria","apellidos":"Caja Prueba","tipoDocumento":"CEDULA","numeroDocumento":"1047000231",
                 "email":"gloria@correo.test","telefono":"+57 300 000 0000","autorizaTratamientoDatos":true}""")
                .andExpect(status().isCreated()));
        LocalDate hoy = LocalDate.now();
        String reserva = """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s","canal":"PRESENCIAL",
                 "abonoInicial":{"valor":300000,"metodo":"EFECTIVO"}}""".formatted(habitacionId, huespedId, hoy, hoy.plusDays(4));

        // Sin turno abierto la recepción no puede cobrar: ni reservas con abono ni movimientos de caja
        enviar(post("/api/reservas"), recepcion, reserva).andExpect(status().isUnprocessableEntity());
        enviar(post("/api/caja/movimientos"), recepcion, """
                {"tipo":"EGRESO","categoria":"OTROS_EGRESOS","concepto":"Sin turno","valor":1000,"metodoPago":"EFECTIVO"}""")
                .andExpect(status().isUnprocessableEntity());

        // Apertura con la base recibida
        long turnoId = id(enviar(post("/api/caja/abrir"), recepcion, """
                {"baseInicial":200000,"observacion":"Base recibida de la administración"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.cajero").value("recepcion")));
        // Una sola caja: otro cajero no la abre ni cobra mientras esté a cargo de otra persona
        enviar(post("/api/caja/abrir"), cajera, """
                {"baseInicial":0}""").andExpect(status().isConflict());
        enviar(post("/api/caja/movimientos"), cajera, """
                {"tipo":"INGRESO","categoria":"OTROS_INGRESOS","concepto":"Ajeno","valor":1000,"metodoPago":"EFECTIVO"}""")
                .andExpect(status().isUnprocessableEntity());

        // Pagos del turno: abono en efectivo al reservar y otro por transferencia
        long reservaId = id(enviar(post("/api/reservas"), recepcion, reserva).andExpect(status().isCreated()));
        enviar(post("/api/reservas/" + reservaId + "/abonos"), recepcion, """
                {"valor":50000,"metodo":"TRANSFERENCIA","referencia":"TRF-1"}""").andExpect(status().isCreated());
        // Egreso pagado desde la caja (queda también en Finanzas)
        long egresoId = id(enviar(post("/api/caja/movimientos"), recepcion, """
                {"tipo":"EGRESO","categoria":"MANTENIMIENTO","concepto":"Bombillos para el pasillo","valor":20000,
                 "metodoPago":"EFECTIVO","tercero":"Ferretería"}""").andExpect(status().isCreated()));
        // Retiro de efectivo a la caja fuerte y aporte de cambio
        enviar(post("/api/caja/retiros-aportes"), recepcion, """
                {"tipo":"RETIRO","valor":1000000,"motivo":"Demasiado"}""").andExpect(status().isUnprocessableEntity());
        enviar(post("/api/caja/retiros-aportes"), recepcion, """
                {"tipo":"RETIRO","valor":100000,"motivo":"Billetes a la caja fuerte"}""").andExpect(status().isCreated());
        enviar(post("/api/caja/retiros-aportes"), recepcion, """
                {"tipo":"APORTE","valor":10000,"motivo":"Monedas de cambio"}""").andExpect(status().isCreated());

        // Esperado en efectivo: 200.000 + 300.000 − 20.000 − 100.000 + 10.000 = 390.000
        mvc.perform(get("/api/caja/estado").header("Authorization", "Bearer " + recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esMio").value(true))
                .andExpect(jsonPath("$.turnoAbierto.efectivoEsperado").value(390000.0))
                .andExpect(jsonPath("$.turnoAbierto.totalIngresos").value(350000.0))
                .andExpect(jsonPath("$.turnoAbierto.totalEgresos").value(20000.0))
                .andExpect(jsonPath("$.turnoAbierto.retiros").value(100000.0))
                .andExpect(jsonPath("$.turnoAbierto.aportes").value(10000.0))
                .andExpect(jsonPath("$.turnoAbierto.totalesPorMedio[?(@.metodo == 'TRANSFERENCIA')].ingresos").value(hasItem(50000.0)));
        mvc.perform(get("/api/caja/turnos/" + turnoId).header("Authorization", "Bearer " + recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movimientos.length()").value(5));
        // Otro recepcionista no ve el turno ajeno
        mvc.perform(get("/api/caja/turnos/" + turnoId).header("Authorization", "Bearer " + cajera))
                .andExpect(status().isForbidden());

        // Arqueo: 7×50.000 + 20.000 + 10.000 + 5.000 = 385.000 → faltante de 5.000 que debe explicarse
        String conteo = """
                [{"denominacion":50000,"cantidad":7},{"denominacion":20000,"cantidad":1},
                 {"denominacion":10000,"cantidad":1},{"denominacion":5000,"cantidad":1}]""";
        enviar(post("/api/caja/cerrar"), recepcion, """
                {"conteo":%s,"baseSiguiente":150000,"recibidoPor":"Cajera Turno Noche"}""".formatted(conteo))
                .andExpect(status().isUnprocessableEntity());
        enviar(post("/api/caja/cerrar"), recepcion, """
                {"conteo":%s,"baseSiguiente":400000,"observacion":"La base no puede superar lo contado"}""".formatted(conteo))
                .andExpect(status().isUnprocessableEntity());
        enviar(post("/api/caja/cerrar"), recepcion, """
                {"conteo":%s,"baseSiguiente":150000,"recibidoPor":"Cajera Turno Noche",
                 "observacion":"Faltan $5.000 de un vuelto mal entregado"}""".formatted(conteo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.efectivoEsperado").value(390000.0))
                .andExpect(jsonPath("$.efectivoContado").value(385000.0))
                .andExpect(jsonPath("$.diferencia").value(-5000.0))
                .andExpect(jsonPath("$.valorEntregado").value(235000.0))
                .andExpect(jsonPath("$.conteo.length()").value(4));

        // El arqueo firmado no se altera: el egreso del turno cerrado ya no se anula
        enviar(patch("/api/finanzas/movimientos/" + egresoId + "/anular"), finanzas, """
                {"motivo":"Prueba"}""").andExpect(status().isUnprocessableEntity());

        // Entrega: la siguiente cajera recibe la base; si cuenta otra cifra debe explicarlo
        mvc.perform(get("/api/caja/estado").header("Authorization", "Bearer " + cajera))
                .andExpect(jsonPath("$.baseSugerida").value(150000.0));
        enviar(post("/api/caja/abrir"), cajera, """
                {"baseInicial":100000}""").andExpect(status().isUnprocessableEntity());
        enviar(post("/api/caja/abrir"), cajera, """
                {"baseInicial":150000}""").andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseEsperada").value(150000.0));

        // Historial: Finanzas consulta todos los turnos; la recepción solo los suyos
        mvc.perform(get("/api/caja/turnos").header("Authorization", "Bearer " + finanzas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(hasItem((int) turnoId)));
        mvc.perform(get("/api/caja/turnos").header("Authorization", "Bearer " + cajera))
                .andExpect(jsonPath("$.contenido[*].cajero").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("cajera.noche"))));
        // Finanzas consulta pero no opera la caja
        enviar(post("/api/caja/retiros-aportes"), finanzas, """
                {"tipo":"RETIRO","valor":1000,"motivo":"No permitido"}""").andExpect(status().isForbidden());

        CajaDePrueba.cerrarComoAdmin(mvc, admin);
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
