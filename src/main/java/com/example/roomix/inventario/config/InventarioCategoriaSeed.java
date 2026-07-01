package com.example.roomix.inventario.config;

import com.example.roomix.inventario.domain.CategoriaInventario;
import com.example.roomix.inventario.repository.CategoriaInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InventarioCategoriaSeed implements ApplicationRunner {

    private final CategoriaInventarioRepository categoriaInventarioRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoriaInventarioRepository.count() > 0) {
            return;
        }

        crear(
                "Limpieza",
                "Productos y herramientas para la limpieza del hotel",
                "Artículos de aseo, jabón líquido, desinfectante, trapeadores, escobas, paños, guantes, cubetas"
        );
        crear(
                "Mobiliario",
                "Todo lo que se encuentra dentro de una habitación o área común",
                "Camas, colchones, almohadas de cama, mesas de noche, sillas, lámparas, espejos, armarios, cortinas"
        );
        crear(
                "Comida",
                "Insumos del restaurante o cocina cuando el hotel maneja alimentos",
                "Ingredientes, bebidas, condimentos, empaques, utensilios de cocina, productos refrigerados"
        );
        crear(
                "Ropa blanca",
                "Textiles de uso en habitaciones y baños",
                "Toallas, sábanas, fundas, cobijas, batas, tapetes de baño"
        );
        crear(
                "Tecnología",
                "Equipos electrónicos y conectividad",
                "Routers, controles remotos, cables HDMI, teléfonos, cargadores, smart TV"
        );
        crear(
                "Amenidades",
                "Suministros de cortesía para el huésped",
                "Jabón, shampoo, acondicionador, kit dental, café, agua embotellada"
        );
        crear(
                "Mantenimiento",
                "Herramientas y repuestos para reparaciones",
                "Bombillos, llaves inglesas, pintura, selladores, fusibles, taladro"
        );
    }

    private void crear(String nombre, String descripcion, String ejemplos) {
        categoriaInventarioRepository.save(CategoriaInventario.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .ejemplosArticulos(ejemplos)
                .activo(true)
                .predefinida(true)
                .build());
    }
}
