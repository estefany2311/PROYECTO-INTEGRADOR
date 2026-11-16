CREATE TABLE estudiante (
    id        BIGSERIAL PRIMARY KEY,
    nombres   VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    aula_id   BIGINT   NOT NULL REFERENCES aula(id) ON DELETE CASCADE,
    grado_id  SMALLINT NOT NULL REFERENCES grado(id),
    activo    BOOLEAN  NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT now(),
    -- el grado del alumno debe ser uno de los que atiende su aula
    FOREIGN KEY (aula_id, grado_id) REFERENCES aula_grado (aula_id, grado_id)
);

CREATE INDEX idx_estudiante_aula ON estudiante (aula_id);

-- sin alumnos duplicados dentro de la misma aula
CREATE UNIQUE INDEX uq_estudiante_aula_nombre
    ON estudiante (aula_id, lower(nombres), lower(apellidos));