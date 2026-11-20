-- T-416 (US-13): estado de la transcripción de cada lectura en voz alta
ALTER TABLE evidencia_lectura
    ADD COLUMN estado_ia     VARCHAR(12) NOT NULL DEFAULT 'procesando'
                             CHECK (estado_ia IN ('procesando', 'listo', 'error')),
    ADD COLUMN transcripcion TEXT,
    ADD COLUMN error_ia      VARCHAR(300),
    ADD COLUMN procesado_en  TIMESTAMP;

-- Cada palabra que escuchó Whisper, en orden; T-422 las alinea con el texto de la lectura
CREATE TABLE evidencia_palabra (
    evidencia_id BIGINT       NOT NULL,
    orden        SMALLINT     NOT NULL,
    palabra      VARCHAR(60)  NOT NULL,
    inicio       NUMERIC(7,2) NOT NULL,
    fin          NUMERIC(7,2) NOT NULL,
    probabilidad NUMERIC(4,3) NOT NULL,
    PRIMARY KEY (evidencia_id, orden)
);
