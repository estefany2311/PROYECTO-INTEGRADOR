-- T-194 (US-37): grupos de alumnos dentro de un aula.
-- Un grupo sirve para asignar una misma lectura a varios alumnos a la vez (T-195).
CREATE TABLE grupo (
    id          BIGSERIAL PRIMARY KEY,
    -- si se borra el aula, sus grupos se van con ella
    aula_id     BIGINT      NOT NULL REFERENCES aula (id) ON DELETE CASCADE,
    nombre      VARCHAR(60) NOT NULL,
    descripcion VARCHAR(200),
    creado_en   TIMESTAMP   NOT NULL DEFAULT now(),
    -- dos grupos con el mismo nombre en el aula confundirían al docente
    UNIQUE (aula_id, nombre)
);

-- consulta más frecuente: los grupos de un aula
CREATE INDEX idx_grupo_aula ON grupo (aula_id);

-- tabla intermedia: qué alumnos forman cada grupo
CREATE TABLE grupo_alumno (
    grupo_id      BIGINT NOT NULL REFERENCES grupo (id) ON DELETE CASCADE,
    estudiante_id BIGINT NOT NULL REFERENCES estudiante (id) ON DELETE CASCADE,
    -- un alumno no puede estar dos veces en el mismo grupo
    PRIMARY KEY (grupo_id, estudiante_id)
);

-- consulta de la tablet: los grupos a los que pertenece un alumno
CREATE INDEX idx_grupo_alumno_estudiante ON grupo_alumno (estudiante_id);
