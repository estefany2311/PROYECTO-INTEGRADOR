-- T-275 (US-27): evaluaciones del aula para el dashboard del docente.
-- Una fila = una evaluacion de una competencia (lectura, escritura u oralidad).
CREATE TABLE evaluacion (
    id            BIGSERIAL   PRIMARY KEY,
    estudiante_id BIGINT      NOT NULL REFERENCES estudiante (id) ON DELETE CASCADE,
    competencia   VARCHAR(20) NOT NULL
                  CHECK (competencia IN ('lectura', 'escritura', 'oralidad')),
    puntaje       SMALLINT    NOT NULL CHECK (puntaje BETWEEN 0 AND 100),
    evaluado_en   TIMESTAMP   NOT NULL DEFAULT now()
);

-- consulta del dashboard: evaluaciones de un alumno por competencia
CREATE INDEX idx_evaluacion_estudiante ON evaluacion (estudiante_id, competencia);

-- Datos de prueba del aula "Noemi De La Cruz": 3 alumnos x 3 competencias.
-- Se insertan solo si el aula y los alumnos existen; en el CI la base esta vacia.
INSERT INTO evaluacion (estudiante_id, competencia, puntaje, evaluado_en)
SELECT e.id, v.competencia, v.puntaje, now() - (v.dias_atras * interval '1 day')
FROM estudiante e
JOIN aula a ON a.id = e.aula_id
JOIN (VALUES
    ('minji prueba', 'delacruz', 'lectura',   90, 21),
    ('minji prueba', 'delacruz', 'escritura', 85, 14),
    ('minji prueba', 'delacruz', 'oralidad',  80,  7),
    ('Breysi',       'Zavala',   'lectura',   75, 21),
    ('Breysi',       'Zavala',   'escritura', 68, 14),
    ('Breysi',       'Zavala',   'oralidad',  62,  7),
    ('Ester',        'Zavala',   'lectura',   55, 21),
    ('Ester',        'Zavala',   'escritura', 50, 14),
    ('Ester',        'Zavala',   'oralidad',  45,  7)
) AS v (nombres, apellidos, competencia, puntaje, dias_atras)
  ON lower(e.nombres) = lower(v.nombres)
 AND lower(e.apellidos) = lower(v.apellidos)
WHERE a.nombre = 'Noemi De La Cruz';
