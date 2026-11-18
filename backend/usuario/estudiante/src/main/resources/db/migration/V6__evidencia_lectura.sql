-- T-414 (US-13): datos de la lectura en voz alta de cada evidencia de audio.
-- La tabla evidencia es del módulo docente (V10 de Noemí); aquí va solo lo propio de la lectura.
-- Sin llaves foráneas, igual que V3, para que el CI con la base vacía no falle.
CREATE TABLE evidencia_lectura (
    evidencia_id  BIGINT    PRIMARY KEY,
    lectura_id    BIGINT    NOT NULL,
    texto_lectura TEXT      NOT NULL,   -- copia del texto leído: si editan la lectura, el puntaje sigue cuadrando
    duracion_ms   INT       NOT NULL CHECK (duracion_ms > 0),
    tamano_bytes  INT       NOT NULL CHECK (tamano_bytes > 0),
    creado_en     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_evidencia_lectura_lectura ON evidencia_lectura (lectura_id);
