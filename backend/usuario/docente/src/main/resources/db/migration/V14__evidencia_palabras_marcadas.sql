-- T-375 (US-32): palabras que la IA marco en la produccion del alumno.
-- Se guardan separadas por comas en una sola columna de texto; el detalle de
-- la evidencia (T-371) las devuelve como lista para resaltarlas en el portal.
-- En las evidencias de audio son las palabras de la transcripcion que la IA
-- quiere que el docente revise (pronunciacion); en las de texto, las que
-- presentan alguna observacion de escritura.
ALTER TABLE evidencia
    ADD COLUMN palabras_marcadas TEXT;

-- Datos de prueba: palabras marcadas en las transcripciones de V13.
-- Se actualiza por el audio, sin quemar ids, y solo si la evidencia existe
-- (en el CI la base esta vacia).
UPDATE evidencia
SET palabras_marcadas = 'brilla,juegan'
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3';

UPDATE evidencia
SET palabras_marcadas = 'conejo,despegaba'
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/minji-nivel3.mp3';

UPDATE evidencia
SET palabras_marcadas = 'abrazo,días'
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/breysi-nivel1.mp3';

UPDATE evidencia
SET palabras_marcadas = 'patito,burlaban'
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/ester-nivel3.mp3';

UPDATE evidencia
SET palabras_marcadas = 'vaca,pasto'
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/ester-nivel1.mp3';
