CREATE TABLE grado (
    id     SMALLSERIAL PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL UNIQUE,
    orden  SMALLINT NOT NULL
);

INSERT INTO grado (nombre, orden) VALUES
('1.º', 1), ('2.º', 2), ('3.º', 3), ('4.º', 4), ('5.º', 5), ('6.º', 6);

CREATE TABLE aula (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    docente_id BIGINT NOT NULL,
    creado_en  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (docente_id, nombre)
);

CREATE TABLE aula_grado (
    aula_id  BIGINT   NOT NULL REFERENCES aula(id) ON DELETE CASCADE,
    grado_id SMALLINT NOT NULL REFERENCES grado(id),
    PRIMARY KEY (aula_id, grado_id)
);