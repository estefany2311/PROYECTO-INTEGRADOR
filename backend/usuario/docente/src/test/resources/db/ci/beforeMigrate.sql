-- SOLO PARA EL CI (base vacía de GitHub Actions). En Neon NO se ejecuta.
-- La tabla `lecturas` la crea el módulo Admin de Daniela (Django). En el CI no existe,
-- y la migración V6 (asignacion) la necesita para su llave foránea.
-- Columnas iguales a backend/admin/catalogo/models.py (rama admin-daniela).
CREATE TABLE IF NOT EXISTS lecturas (
    id             BIGSERIAL    PRIMARY KEY,
    titulo         VARCHAR(150) NOT NULL,
    texto          TEXT         NOT NULL,
    tipo_reto      VARCHAR(20)  NOT NULL,
    grado          SMALLINT     NOT NULL DEFAULT 1 CHECK (grado BETWEEN 1 AND 6),
    dificultad     VARCHAR(20)  NOT NULL DEFAULT 'basica',
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_por_id  BIGINT,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now()
);
