-- Migración manual: PENDIENTE_LIMPIEZA → INHABILITADO (PostgreSQL)
-- Ejecutar si el backend no pudo aplicar la migración al arrancar:
--   psql -U postgres -d roomix -f migration-inhabilitado.sql

ALTER TABLE habitaciones DROP CONSTRAINT IF EXISTS habitaciones_estado_check;
ALTER TABLE habitaciones DROP CONSTRAINT IF EXISTS chk_habitacion_estado;

ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS motivo_inhabilitacion VARCHAR(30);

UPDATE habitaciones
SET motivo_inhabilitacion = CASE
    WHEN motivo_pendiente_limpieza = 'ADECUACION_PROGRAMADA' THEN 'ADECUACION_PROGRAMADA'
    ELSE 'POST_CHECKOUT'
END
WHERE motivo_inhabilitacion IS NULL
  AND motivo_pendiente_limpieza IS NOT NULL;

UPDATE habitaciones
SET motivo_inhabilitacion = 'POST_CHECKOUT'
WHERE motivo_inhabilitacion IS NULL
  AND estado IN ('PENDIENTE_LIMPIEZA', 'INHABILITADO');

UPDATE habitaciones SET estado = 'INHABILITADO' WHERE estado = 'PENDIENTE_LIMPIEZA';

ALTER TABLE habitaciones
    ADD CONSTRAINT habitaciones_estado_check CHECK (
        estado IN ('LIBRE', 'RESERVADO', 'OCUPADO', 'INHABILITADO')
    );

ALTER TABLE habitaciones DROP CONSTRAINT IF EXISTS habitaciones_motivo_pendiente_limpieza_check;
ALTER TABLE habitaciones DROP CONSTRAINT IF EXISTS chk_habitacion_motivo_limpieza;
ALTER TABLE habitaciones DROP CONSTRAINT IF EXISTS habitaciones_motivo_inhabilitacion_check;

ALTER TABLE habitaciones
    ADD CONSTRAINT habitaciones_motivo_inhabilitacion_check CHECK (
        motivo_inhabilitacion IS NULL
        OR motivo_inhabilitacion IN ('POST_CHECKOUT', 'ADECUACION_PROGRAMADA')
    );
