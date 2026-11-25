-- T-380 (US-33): historial de calificaciones de una evidencia.
-- Cada aprobación o corrección deja una fila con la nota de la IA, la nota que
-- dejó el docente, el motivo (si corrigió), quién revisó y cuándo. La evidencia
-- guarda solo la última revisión; esta tabla conserva todas.
CREATE TABLE historial_calificacion (
    id           BIGSERIAL   PRIMARY KEY,
    evidencia_id BIGINT      NOT NULL
                 REFERENCES evidencia (id) ON DELETE CASCADE,
    docente_id   BIGINT      NOT NULL,
    nota_ia      SMALLINT    CHECK (nota_ia BETWEEN 0 AND 100),
    nota_docente SMALLINT    NOT NULL CHECK (nota_docente BETWEEN 0 AND 100),
    motivo       TEXT,
    accion       VARCHAR(15) NOT NULL
                 CHECK (accion IN ('aprobada', 'corregida')),
    creado_en    TIMESTAMP   NOT NULL DEFAULT now()
);

-- El detalle y el historial consultan siempre por evidencia, del más reciente
-- al más antiguo.
CREATE INDEX idx_historial_calificacion_evidencia
    ON historial_calificacion (evidencia_id, creado_en DESC);
