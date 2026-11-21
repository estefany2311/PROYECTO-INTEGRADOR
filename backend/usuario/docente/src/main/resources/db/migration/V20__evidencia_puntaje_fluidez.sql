-- T-396 (US-15): puntaje de fluidez lectora (ritmo y pausas) de una lectura en
-- voz alta, de 0 a 100. Es nulo mientras no se haya calculado y en las
-- evidencias de texto, que no tienen fluidez.
ALTER TABLE evidencia
    ADD COLUMN puntaje_fluidez SMALLINT
        CHECK (puntaje_fluidez BETWEEN 0 AND 100);
