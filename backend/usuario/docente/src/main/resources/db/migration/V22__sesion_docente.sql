-- Login docente: una fila por inicio de sesión en el portal.
-- Se guardan las huellas SHA-256 de los tokens, nunca los tokens.
-- docente_id es el id de la cuenta en la tabla usuarios del módulo Admin. No
-- lleva llave foránea porque esa tabla es de Admin y el CI corre sin ella.
CREATE TABLE sesion_docente (
    id             BIGSERIAL   PRIMARY KEY,
    docente_id     BIGINT      NOT NULL,
    acceso_hash    CHAR(64)    NOT NULL UNIQUE,
    acceso_expira  TIMESTAMP   NOT NULL,
    refresh_hash   CHAR(64)    NOT NULL UNIQUE,
    refresh_expira TIMESTAMP   NOT NULL,
    creado_en      TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_sesion_docente_expira ON sesion_docente (refresh_expira);
