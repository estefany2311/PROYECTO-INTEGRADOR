-- T-308 (US-08): cada vez que el alumno termina un reto queda registrado su intento,
-- aunque no haya sacado estrellas. Sirve para el historial del docente y para no perder
-- lo ganado al repetir un reto (T-310).
-- estudiante_id y nivel_id no tienen llave foránea porque esas tablas son del módulo docente
-- (igual que en V1 y V2).
CREATE TABLE intento_reto (
    id            BIGSERIAL PRIMARY KEY,
    estudiante_id BIGINT    NOT NULL,
    nivel_id      SMALLINT  NOT NULL,
    puntos        INT       NOT NULL DEFAULT 0 CHECK (puntos >= 0),
    estrellas     SMALLINT  NOT NULL DEFAULT 0 CHECK (estrellas BETWEEN 0 AND 3),
    realizado_en  TIMESTAMP NOT NULL
);

-- consulta más frecuente: los intentos de un alumno en un nivel, del más nuevo al más viejo
CREATE INDEX idx_intento_reto_estudiante_nivel ON intento_reto (estudiante_id, nivel_id, realizado_en DESC);
