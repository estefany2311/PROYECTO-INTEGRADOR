-- T-301 (US-09): puntos y estrellas acumulados por el estudiante en el mapa.
-- Una fila por alumno; el servicio de T-302 la suma al completar un reto con
-- las reglas de puntuación de la US-53.
CREATE TABLE puntaje_estudiante (
    id             BIGSERIAL   PRIMARY KEY,
    estudiante_id  BIGINT      NOT NULL UNIQUE
                   REFERENCES estudiante (id) ON DELETE CASCADE,
    puntos         INT         NOT NULL DEFAULT 0 CHECK (puntos >= 0),
    estrellas      INT         NOT NULL DEFAULT 0 CHECK (estrellas >= 0),
    actualizado_en TIMESTAMP   NOT NULL DEFAULT now()
);
