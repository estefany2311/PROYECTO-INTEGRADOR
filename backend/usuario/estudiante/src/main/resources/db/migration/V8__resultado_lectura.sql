-- T-424 (US-14): resultado de la calificación de cada lectura en voz alta
ALTER TABLE evidencia_lectura
    ADD COLUMN precision_pct SMALLINT CHECK (precision_pct BETWEEN 0 AND 100),
    ADD COLUMN correctas     SMALLINT,
    ADD COLUMN evaluadas     SMALLINT,
    ADD COLUMN cobertura_pct SMALLINT CHECK (cobertura_pct BETWEEN 0 AND 100),
    ADD COLUMN umbral_pct    SMALLINT,
    ADD COLUMN aprobado      BOOLEAN;

-- Cada palabra del texto con lo que leyó el niño; T-428/T-429 resaltan las que debe mejorar
CREATE TABLE evidencia_resultado_palabra (
    evidencia_id BIGINT      NOT NULL,
    orden        SMALLINT    NOT NULL,
    esperada     VARCHAR(60),                     -- null si es una palabra que agregó
    leida        VARCHAR(60),                     -- null si se la saltó
    tipo         VARCHAR(10) NOT NULL CHECK (tipo IN ('correcta', 'cambiada', 'omitida', 'agregada')),
    PRIMARY KEY (evidencia_id, orden)
);
