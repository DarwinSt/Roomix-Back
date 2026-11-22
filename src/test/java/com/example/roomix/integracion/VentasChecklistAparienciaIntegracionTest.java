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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checklists de aseo definidos por el administrador, venta de productos del inventario y apariencia del hotel. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentasChecklistAparienciaIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;

    @Test
    void elAdministradorDefineLasAccionesDeAseoYMantenimiento() throws Exception {
        String admin = token("admin");
        String housekeeping = token("housekeeping");

        enviar(get("/api/incidencias/checklists"), housekeeping, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipo=='LIMPIEZA_URGENCIA')].personalizada").value(hasItem(false)));
        // Solo quien tiene el permiso de configuración cambia los checklists
        enviar(put("/api/incidencias/checklists/MANTENIMIENTO_ZONA_COMUN"), housekeeping, """
                {"tareas":["Revisar"]}""").andExpect(status().isForbidden());
        enviar(put("/api/incidencias/checklists/MANTENIMIENTO_ZONA_COMUN"), admin, """
                {"tareas":["Revisar","revisar"]}""").andExpect(status().isBadRequest());
        enviar(put("/api/incidencias/checklists/MANTENIMIENTO_ZONA_COMUN"), admin, """
                {"tareas":[]}""").andExpect(status().isBadRequest());

        try {
            enviar(put("/api/incidencias/checklists/MANTENIMIENTO_ZONA_COMUN"), admin, """
                    {"tareas":["  Aislar el área  ","Limpiar filtros de la piscina","Medir el cloro"]}""")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personalizada").value(true))
                    .andExpect(jsonPath("$.tareas[0]").value("Aislar el área"));

            // Los servicios nuevos usan las tareas del hotel
            String creada = enviar(post("/api/incidencias"), housekeeping, """
                    {"alcance":"ZONA_COMUN","ubicacion":"Piscina","tipo":"MANTENIMIENTO","descripcion":"Agua turbia"}""")
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            List<String> tareas = JsonPath.read(creada, "$.tareas[*].descripcion");
            assertThat(tareas).containsExactly("Aislar el área", "Limpiar filtros de la piscina", "Medir el cloro");
        } finally {
            enviar(delete("/api/incidencias/checklists/MANTENIMIENTO_ZONA_COMUN"), admin, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personalizada").value(false))
                    .andExpect(jsonPath("$.tareas[0]").value("Revisión inicial del área"));
        }
    }

    @Test
    void ventaDeProductosDeContadoYCargadaALaHabitacion() throws Exception {
        String admin = token("admin");
        String housekeeping = token("housekeeping");
        Number categoriaId = JsonPath.read(enviar(get("/api/inventario/categorias"), admin, null)
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        long gaseosa = id(enviar(post("/api/inventario"), admin, """
                {"nombre":"Gaseosa de prueba 400 ml","descripcion":"Minibar","categoriaId":%d,"cantidad":10,"cantidadMinima":2,
                 "unidadMedida":"UNIDAD","precioUnitario":2500,"precioVenta":4500}""".formatted(categoriaId.longValue()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioVenta").value(4500)));
        long noSeVende = id(enviar(post("/api/inventario"), admin, """
                {"nombre":"Cloro industrial de prueba","descripcion":"Aseo","categoriaId":%d,"cantidad":5,"unidadMedida":"LITRO"}"""
                .formatted(categoriaId.longValue())).andExpect(status().isCreated()));

        enviar(get("/api/ventas/productos"), admin, null)
                .andExpect(jsonPath("$[*].nombre").value(hasItem("Gaseosa de prueba 400 ml")))
                .andExpect(jsonPath("$[*].nombre").value(not(hasItem("Cloro industrial de prueba"))));
        enviar(post("/api/ventas"), housekeeping, venta("CONTADO", null, gaseosa, 1, "EFECTIVO")).andExpect(status().isForbidden());
        enviar(post("/api/ventas"), admin, venta("CONTADO", null, gaseosa, 1, null)).andExpect(status().isUnprocessableEntity());
        enviar(post("/api/ventas"), admin, venta("CONTADO", null, gaseosa, 50, "EFECTIVO")).andExpect(status().isUnprocessableEntity());
        enviar(post("/api/ventas"), admin, venta("CONTADO", null, noSeVende, 1, "EFECTIVO")).andExpect(status().isUnprocessableEntity());

        // De contado: ingreso en Finanzas y salida del inventario
        long contado = id(enviar(post("/api/ventas"), admin, venta("CONTADO", null, gaseosa, 2, "EFECTIVO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(9000))
                .andExpect(jsonPath("$.metodoPagoNombre").value("Efectivo")));
        enviar(get("/api/inventario/" + gaseosa), admin, null).andExpect(jsonPath("$.cantidad").value(8));
        enviar(get("/api/inventario/" + gaseosa + "/movimientos"), admin, null)
                .andExpect(jsonPath("$[0].motivo").value("Venta #" + contado));

        // Cargada a la habitación: requiere huésped alojado y aparece en su cuenta
        long reserva = reservaEnCurso(admin, "952");
        enviar(post("/api/ventas"), admin, venta("CARGO_HABITACION", null, gaseosa, 1, null)).andExpect(status().isUnprocessableEntity());
        long cargo = id(enviar(post("/api/ventas"), admin, venta("CARGO_HABITACION", reserva, gaseosa, 3, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.habitacion").value("952"))
                .andExpect(jsonPath("$.total").value(13500)));
        enviar(get("/api/inventario/" + gaseosa), admin, null).andExpect(jsonPath("$.cantidad").value(5));
        enviar(get("/api/ventas/estadias"), admin, null).andExpect(jsonPath("$[*].reservaId").value(hasItem((int) reserva)));
        enviar(get("/api/reservas/" + reserva + "/cuenta"), admin, null)
                .andExpect(jsonPath("$.lineas[?(@.origen=='PRODUCTO')].total").value(hasItem(13500.0)));

        // Anular devuelve el stock y quita el cargo de la cuenta
        enviar(patch("/api/ventas/" + cargo + "/anular"), admin, "{\"motivo\":\"Cargada a la habitación equivocada\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ANULADA"));
        enviar(get("/api/reservas/" + reserva + "/cuenta"), admin, null)
                .andExpect(jsonPath("$.lineas[*].origen").value(not(hasItem("PRODUCTO"))));
        enviar(patch("/api/ventas/" + contado + "/anular"), admin, "{\"motivo\":\"Devolución\"}").andExpect(status().isOk());
        enviar(patch("/api/ventas/" + contado + "/anular"), admin, "{\"motivo\":\"Otra vez\"}").andExpect(status().isConflict());
        enviar(get("/api/inventario/" + gaseosa), admin, null).andExpect(jsonPath("$.cantidad").value(10));
        enviar(get("/api/ventas?reservaId=" + reserva), admin, null).andExpect(jsonPath("$[0].estado").value("ANULADA"));
    }

    @Test
    void reporteDeVentasPorProductoConCostoYMargen() throws Exception {
        String admin = token("admin");
        Number categoriaId = JsonPath.read(enviar(get("/api/inventario/categorias"), admin, null)
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        long chocolatina = id(enviar(post("/api/inventario"), admin, """
                {"nombre":"Chocolatina de reporte","descripcion":"Minibar","categoriaId":%d,"cantidad":10,"unidadMedida":"UNIDAD",
                 "precioUnitario":2000,"precioVenta":5000}""".formatted(categoriaId.longValue())).andExpect(status().isCreated()));
        long venta = id(enviar(post("/api/ventas"), admin, venta("CONTADO", null, chocolatina, 2, "EFECTIVO"))
                .andExpect(status().isCreated()));

        // Ticket: la venta completa por su número
        enviar(get("/api/ventas/" + venta), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].nombre").value("Chocolatina de reporte"));

        // El costo se guarda al vender: si después cambia, el margen de esta venta no cambia
        enviar(get("/api/reportes/ventas-productos"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos[?(@.articuloId==%d)].ingresos".formatted(chocolatina)).value(hasItem(10000.0)))
                .andExpect(jsonPath("$.productos[?(@.articuloId==%d)].costo".formatted(chocolatina)).value(hasItem(4000.0)))
                .andExpect(jsonPath("$.productos[?(@.articuloId==%d)].margen".formatted(chocolatina)).value(hasItem(6000.0)));
        enviar(get("/api/reportes/ventas-productos"), token("recepcion"), null).andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorPersonalizaLosColoresDelHotel() throws Exception {
        String admin = token("admin");
        enviar(put("/api/configuracion/apariencia"), token("recepcion"), """
                {"colorPrimario":"#1E3A5F"}""").andExpect(status().isForbidden());
        enviar(put("/api/configuracion/apariencia"), admin, """
                {"colorPrimario":"azul"}""").andExpect(status().isBadRequest());
        try {
            enviar(put("/api/configuracion/apariencia"), admin, """
                    {"colorPrimario":"#1E3A5F","colorAcento":"#E0A030","eslogan":"Descanso frente al río"}""")
                    .andExpect(status().isOk());
            mvc.perform(get("/api/configuracion/publica"))
                    .andExpect(jsonPath("$.colorPrimario").value("#1e3a5f"))
                    .andExpect(jsonPath("$.colorAcento").value("#e0a030"))
                    .andExpect(jsonPath("$.eslogan").value("Descanso frente al río"));
        } finally {
            enviar(put("/api/configuracion/apariencia"), admin, "{}").andExpect(status().isOk());
        }
        mvc.perform(get("/api/configuracion/publica")).andExpect(jsonPath("$.colorPrimario").doesNotExist());
    }

    private long reservaEnCurso(String admin, String numero) throws Exception {
        long habitacionId = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"%s","tipoHabitacion":"DOBLE","descripcion":"Habitación de ventas","precioNoche":100000}""".formatted(numero))
                .andExpect(status().isCreated()));
        long huespedId = id(enviar(post("/api/huespedes"), admin, """
                {"nombre":"Lucía","apellidos":"Minibar Prueba","tipoDocumento":"CEDULA","numeroDocumento":"1047000952",
                 "email":"lucia@correo.test","telefono":"+57 300 000 0000","autorizaTratamientoDatos":true}""")
                .andExpect(status().isCreated()));
        LocalDate hoy = LocalDate.now();
        long reserva = id(enviar(post("/api/reservas"), admin, """
                {"habitacionId":%d,"huespedId":%d,"fechaEntrada":"%s","fechaSalida":"%s","canal":"PRESENCIAL",
                 "abonoInicial":{"valor":200000,"metodo":"EFECTIVO"}}""".formatted(habitacionId, huespedId, hoy, hoy.plusDays(2)))
                .andExpect(status().isCreated()));
        enviar(patch("/api/reservas/" + reserva + "/check-in"), admin, null).andExpect(status().isOk());
        return reserva;
    }

    private static String venta(String modo, Long reserva, long articulo, int cantidad, String metodo) {
        return """
                {"modo":"%s","reservaId":%s,"items":[{"articuloId":%d,"cantidad":%d}],"metodoPago":%s,"cliente":"Cliente de paso"}"""
                .formatted(modo, reserva, articulo, cantidad, metodo == null ? "null" : "\"" + metodo + "\"");
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
