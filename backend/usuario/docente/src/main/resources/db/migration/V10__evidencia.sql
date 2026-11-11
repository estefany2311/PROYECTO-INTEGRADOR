-- T-296 (US-31): evidencias de audio y texto para la revision del docente.
-- La tabla la comparten los modulos docente y estudiante: Karla registra la
-- evidencia desde la tablet y el docente la revisa en US-31.
CREATE TABLE evidencia (
    id              BIGSERIAL   PRIMARY KEY,
    estudiante_id   BIGINT      NOT NULL REFERENCES estudiante (id) ON DELETE CASCADE,
    nivel_id        SMALLINT    NOT NULL REFERENCES nivel (id) ON DELETE CASCADE,
    tipo            VARCHAR(10) NOT NULL CHECK (tipo IN ('audio', 'texto')),
    audio_url       VARCHAR(500),
    texto           TEXT,
    resultado_ia    TEXT,
    estado_revision VARCHAR(15) NOT NULL DEFAULT 'pendiente'
                    CHECK (estado_revision IN ('pendiente', 'aprobada', 'rechazada')),
    creado_en       TIMESTAMP   NOT NULL DEFAULT now(),
    revisado_en     TIMESTAMP,
    -- una evidencia de audio necesita su URL y una de texto su contenido
    CONSTRAINT chk_evidencia_contenido CHECK (
        (tipo = 'audio' AND audio_url IS NOT NULL) OR
        (tipo = 'texto' AND texto IS NOT NULL)
    )
);

-- evidencias de un alumno y listado de pendientes por fecha
CREATE INDEX idx_evidencia_estudiante ON evidencia (estudiante_id);
CREATE INDEX idx_evidencia_revision ON evidencia (estado_revision, creado_en DESC);
