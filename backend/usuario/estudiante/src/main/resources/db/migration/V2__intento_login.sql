-- T-203: intentos fallidos de PIN y bloqueo temporal por estudiante.
CREATE TABLE intento_login (
    estudiante_id   BIGINT    PRIMARY KEY,
    fallidos        SMALLINT  NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMP
);
