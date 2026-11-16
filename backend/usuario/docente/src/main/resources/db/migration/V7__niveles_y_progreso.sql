-- T-197 (US-05): mapa de aventura del estudiante.
-- Karla delegó esta tarea: las tablas viven en mi historial de Flyway.
CREATE TABLE nivel (
    id          SMALLSERIAL PRIMARY KEY,
    orden       SMALLINT    NOT NULL UNIQUE,   -- recorrido del mapa: 1, 2, 3...
    nombre      VARCHAR(60) NOT NULL,
    tipo_reto   VARCHAR(20) NOT NULL
                CHECK (tipo_reto IN ('lectura', 'escritura', 'oralidad')),
    descripcion VARCHAR(200),
    activo      BOOLEAN     NOT NULL DEFAULT TRUE
);

-- Datos semilla: 6 niveles, alternando los 3 tipos de reto
INSERT INTO nivel (orden, nombre, tipo_reto, descripcion) VALUES
(1, 'Primeros pasos',  'lectura',    'Lee palabras y oraciones cortas'),
(2, 'Palabras nuevas', 'escritura',  'Escribe palabras y frases sencillas'),
(3, 'Cuento corto',    'oralidad',   'Cuenta un cuento con tus palabras'),
(4, 'Idea principal',  'lectura',    'Encuentra la idea principal del texto'),
(5, 'Mi relato',       'escritura',  'Escribe un relato breve'),
(6, 'Mi exposición',   'oralidad',   'Explica un tema frente a tus compañeros');

-- Estado de cada alumno en cada nivel (una fila por alumno y nivel)
CREATE TABLE progreso (
    id             BIGSERIAL PRIMARY KEY,
    estudiante_id  BIGINT      NOT NULL REFERENCES estudiante (id) ON DELETE CASCADE,
    nivel_id       SMALLINT    NOT NULL REFERENCES nivel (id) ON DELETE CASCADE,
    estado         VARCHAR(15) NOT NULL DEFAULT 'bloqueado'
                   CHECK (estado IN ('bloqueado', 'desbloqueado', 'completado')),
    puntos         INT         NOT NULL DEFAULT 0,
    actualizado_en TIMESTAMP   NOT NULL DEFAULT now(),
    -- un solo estado por alumno y nivel
    UNIQUE (estudiante_id, nivel_id)
);

CREATE INDEX idx_progreso_estudiante ON progreso (estudiante_id);
