-- T-202: sesiones del estudiante en la tablet.
-- Se guarda el SHA-256 del token, nunca el token en claro.
-- estudiante_id no tiene llave foránea porque la tabla estudiante es del módulo docente.
CREATE TABLE sesion_estudiante (
                                   token_hash    CHAR(64)  PRIMARY KEY,
                                   estudiante_id BIGINT    NOT NULL,
                                   creada_en     TIMESTAMP NOT NULL DEFAULT now(),
                                   expira_en     TIMESTAMP NOT NULL
);

CREATE INDEX idx_sesion_estudiante_estudiante ON sesion_estudiante (estudiante_id);