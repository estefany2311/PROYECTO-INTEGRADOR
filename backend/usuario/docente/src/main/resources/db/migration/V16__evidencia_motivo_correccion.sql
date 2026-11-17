-- T-378 (US-33): el docente puede corregir la calificación de la IA y debe
-- explicar por qué. El motivo se guarda en la propia evidencia; el historial
-- completo de notas (IA vs. docente) llega en T-380.
ALTER TABLE evidencia
    ADD COLUMN motivo_correccion TEXT;
