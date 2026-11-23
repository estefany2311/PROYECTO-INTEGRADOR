-- T-425 (US-14): estrellas y puntos del intento, y el nivel que desbloqueó (si fue la primera vez)
ALTER TABLE evidencia_lectura
    ADD COLUMN estrellas          SMALLINT CHECK (estrellas BETWEEN 0 AND 3),
    ADD COLUMN puntos             INT      CHECK (puntos >= 0),
    ADD COLUMN nivel_desbloqueado SMALLINT;
