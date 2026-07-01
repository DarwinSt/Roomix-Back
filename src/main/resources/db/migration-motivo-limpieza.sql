-- Añade motivo de limpieza si la tabla se creó antes de este campo.
-- Ejecutar: psql -U postgres -d roomix -f migration-motivo-limpieza.sql

ALTER TABLE habitaciones
    ADD COLUMN IF NOT EXISTS motivo_pendiente_limpieza VARCHAR(30);

ALTER TABLE habitaciones DROP CONSTRAINT IF EXISTS chk_habitacion_motivo_limpieza;

ALTER TABLE habitaciones ADD CONSTRAINT chk_habitacion_motivo_limpieza CHECK (
    motivo_pendiente_limpieza IS NULL
    OR motivo_pendiente_limpieza IN ('SERVICIO_HUESPED', 'POST_CHECKOUT')
);

-- Habitaciones ya en limpieza sin motivo: asumir post check-out (admin)
UPDATE habitaciones
SET motivo_pendiente_limpieza = 'POST_CHECKOUT'
WHERE estado = 'PENDIENTE_LIMPIEZA'
  AND motivo_pendiente_limpieza IS NULL;
