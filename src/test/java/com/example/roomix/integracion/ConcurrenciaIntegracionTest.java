package com.example.roomix.integracion;

import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.inventario.repository.ArticuloInventarioRepository;
import com.example.roomix.reserva.dto.CrearReservaRequest;
import com.example.roomix.reserva.service.ReservaService;
import com.example.roomix.venta.domain.ModoVenta;
import com.example.roomix.venta.dto.VentaDtos.ItemVentaRequest;
import com.example.roomix.venta.dto.VentaDtos.VentaRequest;
import com.example.roomix.venta.service.VentaService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Varios usuarios al mismo tiempo sobre el mismo registro: la misma habitación y fechas, o el último stock de un
 * producto. El sistema debe dejar pasar solo lo que de verdad cabe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrenciaIntegracionTest {

    private static final String CLAVE = "Roomix2026*";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ReservaService reservaService;
    @Autowired
    private VentaService ventaService;
    @Autowired
    private ArticuloInventarioRepository articuloRepository;

    @Test
    void dosRecepcionistasNoPuedenReservarLaMismaHabitacionYFechas() throws Exception {
        String admin = token("admin");
        long habitacion = id(enviar(post("/api/habitaciones"), admin, """
                {"numero":"961","tipoHabitacion":"DOBLE","descripcion":"Habitación de concurrencia","precioNoche":100000}""")
                .andExpect(status().isCreated()));
        List<Long> huespedes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            huespedes.add(id(enviar(post("/api/huespedes"), admin, """
                    {"nombre":"Huésped","apellidos":"Concurrente %d","tipoDocumento":"CEDULA","numeroDocumento":"104700096%d",
                     "email":"concurrente%d@correo.test","telefono":"+57 300 000 0000","autorizaTratamientoDatos":true}"""
                    .formatted(i, i, i)).andExpect(status().isCreated())));
        }
        LocalDate entrada = LocalDate.now().plusDays(20);

        List<Callable<Long>> tareas = new ArrayList<>();
        for (Long huesped : huespedes) {
            tareas.add(() -> reservaService.crear(new CrearReservaRequest(habitacion, huesped, entrada, entrada.plusDays(2), null, null)).id());
        }
        Resultado r = enParalelo(tareas);

        assertThat(r.exitos()).as("solo una reserva entra para la misma habitación y fechas").isEqualTo(1);
        assertThat(r.fallos()).isEqualTo(huespedes.size() - 1);
    }

    @Test
    void variasVentasSimultaneasNoVendenMasDeLoQueHay() throws Exception {
        String admin = token("admin");
        Number categoriaId = JsonPath.read(enviar(get("/api/inventario/categorias"), admin, null)
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        long papas = id(enviar(post("/api/inventario"), admin, """
                {"nombre":"Papas de concurrencia","descripcion":"Últimas unidades","categoriaId":%d,"cantidad":3,
                 "unidadMedida":"UNIDAD","precioVenta":3500}""".formatted(categoriaId.longValue())).andExpect(status().isCreated()));

        List<Callable<Long>> tareas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tareas.add(() -> ventaService.registrar(new VentaRequest(ModoVenta.CONTADO, null,
                    List.of(new ItemVentaRequest(papas, 1)), MetodoPago.EFECTIVO, null, null)).id());
        }
        Resultado r = enParalelo(tareas);

        int quedan = articuloRepository.findById(papas).orElseThrow().getCantidad();
        assertThat(r.exitos()).as("no se venden más de 3 unidades").isLessThanOrEqualTo(3).isPositive();
        assertThat(quedan).as("el stock nunca queda negativo y cuadra con lo vendido").isEqualTo(3 - r.exitos());
    }

    private record Resultado(int exitos, int fallos) {
    }

    /** Ejecuta las tareas a la vez (todas esperan la misma señal de salida), cada una autenticada como el administrador. */
    private Resultado enParalelo(List<Callable<Long>> tareas) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Long>> futuros = new ArrayList<>();
        for (Callable<Long> tarea : tareas) {
            futuros.add(hilos.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
                try {
                    salida.await();
                    return tarea.call();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }
        salida.countDown();
        int exitos = 0;
        int fallos = 0;
        for (Future<Long> f : futuros) {
            try {
                f.get(30, TimeUnit.SECONDS);
                exitos++;
            } catch (java.util.concurrent.ExecutionException e) {
                fallos++;
            }
        }
        hilos.shutdownNow();
        return new Resultado(exitos, fallos);
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
