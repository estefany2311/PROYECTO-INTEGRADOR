-- Puntos y estrellas por alumno y nivel: se guarda el MEJOR intento.
-- puntaje_estudiante (V12) sigue siendo el total del alumno; esta tabla recuerda
-- cuánto de ese total viene de cada nivel, para sumar solo la diferencia cuando
-- el alumno mejora (o cuando el docente corrige la nota) y no contar dos veces
-- el mismo nivel.
CREATE TABLE puntaje_nivel_estudiante (
    id             BIGSERIAL PRIMARY KEY,
    estudiante_id  BIGINT    NOT NULL REFERENCES estudiante (id) ON DELETE CASCADE,
    nivel_id       SMALLINT  NOT NULL REFERENCES nivel (id) ON DELETE CASCADE,
    -- nota de 0 a 100 del mejor intento del alumno en ese nivel
    nota           SMALLINT  NOT NULL CHECK (nota BETWEEN 0 AND 100),
    -- puntos y estrellas que esa nota dio con las reglas vigentes (US-53)
    puntos         INT       NOT NULL DEFAULT 0 CHECK (puntos >= 0),
    estrellas      INT       NOT NULL DEFAULT 0 CHECK (estrellas BETWEEN 0 AND 3),
    actualizado_en TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_puntaje_nivel_estudiante UNIQUE (estudiante_id, nivel_id)
);
