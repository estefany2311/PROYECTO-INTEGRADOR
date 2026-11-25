-- T-377 (US-33): el docente aprueba o corrige la calificación que emitió la IA.
-- `calificacion_final` guarda la nota que deja el docente (0 a 100); mientras
-- no revise la evidencia queda nula y la pantalla muestra el puntaje de la IA.
--
-- El estado de revisión suma 'corregida' para distinguir una nota ajustada por
-- el docente de una aprobada tal como la emitió la IA. Se conserva 'rechazada'
-- porque las evidencias ya revisadas en US-31 siguen siendo válidas.
ALTER TABLE evidencia
    ADD COLUMN calificacion_final SMALLINT
        CONSTRAINT chk_evidencia_calificacion_final
        CHECK (calificacion_final BETWEEN 0 AND 100);

-- El CHECK de V10 se creó en línea y Postgres lo nombró
-- `evidencia_estado_revision_check`; se busca por definición para no depender
-- del nombre y se reemplaza por uno que admite el estado nuevo.
DO $$
DECLARE
    nombre TEXT;
BEGIN
    SELECT con.conname INTO nombre
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'evidencia'
      AND con.contype = 'c'
      AND att.attname = 'estado_revision'
      AND pg_get_constraintdef(con.oid) LIKE '%pendiente%';

    IF nombre IS NOT NULL THEN
        EXECUTE format('ALTER TABLE evidencia DROP CONSTRAINT %I', nombre);
    END IF;
END $$;

ALTER TABLE evidencia
    ADD CONSTRAINT chk_evidencia_estado_revision
        CHECK (estado_revision IN ('pendiente', 'aprobada', 'rechazada', 'corregida'));
