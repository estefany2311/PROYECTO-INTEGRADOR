-- T-191: asignación de una lectura del catálogo (tabla de Daniela) a un alumno concreto.
-- Regla: un alumno no puede tener la misma lectura asignada dos veces.
CREATE TABLE asignacion (
    id               BIGSERIAL PRIMARY KEY,
    lectura_id       BIGINT    NOT NULL REFERENCES lecturas (id),
    -- si se borra el alumno, sus asignaciones se van con él
    estudiante_id    BIGINT    NOT NULL REFERENCES estudiante (id) ON DELETE CASCADE,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT now()
);

-- consulta más frecuente: las tareas de un alumno
CREATE INDEX idx_asignacion_estudiante ON asignacion (estudiante_id);

-- sin tareas duplicadas para el mismo alumno
CREATE UNIQUE INDEX uq_asignacion_lectura_estudiante
    ON asignacion (lectura_id, estudiante_id);
