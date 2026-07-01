-- =============================================================================
-- Roomix — Esquema PostgreSQL
-- Base de datos: roomix
--
-- Uso:
--   1. Crear la BD (como superusuario):  psql -U postgres -f init-database.sql
--   2. Crear tablas:                   psql -U postgres -d roomix -f schema.sql
--
-- Compatible con las entidades JPA del backend Spring Boot.
-- =============================================================================

-- Opcional: recrear tablas desde cero (descomenta si necesitas reset limpio)
-- DROP TABLE IF EXISTS habitacion_caracteristicas CASCADE;
-- DROP TABLE IF EXISTS inventario_articulos CASCADE;
-- DROP TABLE IF EXISTS inventario_categorias CASCADE;
-- DROP TABLE IF EXISTS habitaciones CASCADE;

-- -----------------------------------------------------------------------------
-- Habitaciones
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS habitaciones (
    id                              BIGSERIAL PRIMARY KEY,
    numero                          VARCHAR(20)  NOT NULL,
    tipo_habitacion                 VARCHAR(30)  NOT NULL,
    descripcion                     VARCHAR(500) NOT NULL,
    estado                          VARCHAR(30)  NOT NULL DEFAULT 'LIBRE',
    fecha_hora_reservacion          TIMESTAMP,
    motivo_inhabilitacion           VARCHAR(30),
    fecha_hora_ultima_actualizacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_habitacion_numero UNIQUE (numero),
    CONSTRAINT habitaciones_estado_check CHECK (
        estado IN ('LIBRE', 'RESERVADO', 'OCUPADO', 'INHABILITADO')
    ),
    CONSTRAINT chk_habitacion_tipo CHECK (
        tipo_habitacion IN ('INDIVIDUAL', 'DOBLE', 'TRIPLE', 'SUITE', 'FAMILIAR', 'EJECUTIVA')
    ),
    CONSTRAINT habitaciones_motivo_inhabilitacion_check CHECK (
        motivo_inhabilitacion IS NULL
        OR motivo_inhabilitacion IN ('POST_CHECKOUT', 'ADECUACION_PROGRAMADA')
    )
);

CREATE TABLE IF NOT EXISTS habitacion_caracteristicas (
    habitacion_id BIGINT       NOT NULL,
    caracteristica VARCHAR(100) NOT NULL,
    CONSTRAINT fk_habitacion_caracteristicas_habitacion
        FOREIGN KEY (habitacion_id) REFERENCES habitaciones (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_habitacion_caracteristicas_habitacion_id
    ON habitacion_caracteristicas (habitacion_id);

CREATE INDEX IF NOT EXISTS idx_habitaciones_estado ON habitaciones (estado);

-- -----------------------------------------------------------------------------
-- Inventario — categorías (extensibles)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventario_categorias (
    id                              BIGSERIAL PRIMARY KEY,
    nombre                          VARCHAR(80)  NOT NULL,
    descripcion                     VARCHAR(300) NOT NULL,
    ejemplos_articulos              VARCHAR(500) NOT NULL,
    activo                          BOOLEAN      NOT NULL DEFAULT TRUE,
    predefinida                     BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_hora_creacion             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_hora_ultima_actualizacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
);

CREATE INDEX IF NOT EXISTS idx_inventario_categorias_activo ON inventario_categorias (activo);

-- -----------------------------------------------------------------------------
-- Inventario — artículos
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventario_articulos (
    id                              BIGSERIAL PRIMARY KEY,
    nombre                          VARCHAR(120) NOT NULL,
    descripcion                     VARCHAR(500) NOT NULL,
    categoria_id                    BIGINT       NOT NULL,
    cantidad                        INTEGER      NOT NULL DEFAULT 0,
    cantidad_minima                 INTEGER,
    unidad_medida                   VARCHAR(20)  NOT NULL,
    ubicacion                       VARCHAR(150),
    activo                          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_hora_creacion             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_hora_ultima_actualizacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventario_nombre UNIQUE (nombre),
    CONSTRAINT fk_inventario_articulos_categoria
        FOREIGN KEY (categoria_id) REFERENCES inventario_categorias (id),
    CONSTRAINT chk_inventario_cantidad CHECK (cantidad >= 0),
    CONSTRAINT chk_inventario_cantidad_minima CHECK (cantidad_minima IS NULL OR cantidad_minima >= 0),
    CONSTRAINT chk_inventario_unidad CHECK (
        unidad_medida IN ('UNIDAD', 'PAR', 'JUEGO', 'CAJA', 'LITRO', 'KILOGRAMO')
    )
);

CREATE INDEX IF NOT EXISTS idx_inventario_articulos_categoria_id ON inventario_articulos (categoria_id);
CREATE INDEX IF NOT EXISTS idx_inventario_articulos_activo ON inventario_articulos (activo);

-- -----------------------------------------------------------------------------
-- Datos iniciales — categorías de inventario
-- (El backend también las crea con InventarioCategoriaSeed si la tabla está vacía)
-- -----------------------------------------------------------------------------
INSERT INTO inventario_categorias (nombre, descripcion, ejemplos_articulos, activo, predefinida)
VALUES
    (
        'Limpieza',
        'Productos y herramientas para la limpieza del hotel',
        'Artículos de aseo, jabón líquido, desinfectante, trapeadores, escobas, paños, guantes, cubetas',
        TRUE,
        TRUE
    ),
    (
        'Mobiliario',
        'Todo lo que se encuentra dentro de una habitación o área común',
        'Camas, colchones, almohadas de cama, mesas de noche, sillas, lámparas, espejos, armarios, cortinas',
        TRUE,
        TRUE
    ),
    (
        'Comida',
        'Insumos del restaurante o cocina cuando el hotel maneja alimentos',
        'Ingredientes, bebidas, condimentos, empaques, utensilios de cocina, productos refrigerados',
        TRUE,
        TRUE
    ),
    (
        'Ropa blanca',
        'Textiles de uso en habitaciones y baños',
        'Toallas, sábanas, fundas, cobijas, batas, tapetes de baño',
        TRUE,
        TRUE
    ),
    (
        'Tecnología',
        'Equipos electrónicos y conectividad',
        'Routers, controles remotos, cables HDMI, teléfonos, cargadores, smart TV',
        TRUE,
        TRUE
    ),
    (
        'Amenidades',
        'Suministros de cortesía para el huésped',
        'Jabón, shampoo, acondicionador, kit dental, café, agua embotellada',
        TRUE,
        TRUE
    ),
    (
        'Mantenimiento',
        'Herramientas y repuestos para reparaciones',
        'Bombillos, llaves inglesas, pintura, selladores, fusibles, taladro',
        TRUE,
        TRUE
    )
ON CONFLICT (nombre) DO NOTHING;
