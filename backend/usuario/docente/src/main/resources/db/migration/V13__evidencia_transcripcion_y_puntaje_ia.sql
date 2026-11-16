-- T-371 (US-32): detalle de la evidencia para que el docente contraste la
-- evaluación automática. Se agregan la transcripción del audio y el puntaje
-- global que asigna el motor de IA.
--
-- `texto` sigue siendo lo que escribió el alumno en las evidencias de texto y
-- `transcripcion` lo que la IA entendió en las de audio. El puntaje es de 0 a
-- 100 y puede ser nulo mientras la IA no termina de evaluar.
ALTER TABLE evidencia
    ADD COLUMN transcripcion TEXT,
    ADD COLUMN puntaje_ia    SMALLINT
        CONSTRAINT chk_evidencia_puntaje_ia CHECK (puntaje_ia BETWEEN 0 AND 100);

-- Transcripciones y puntajes de las evidencias de prueba de V11.
-- Se actualiza por el audio o por el texto, sin quemar ids, y solo si la
-- evidencia existe (en el CI la base está vacía).
UPDATE evidencia
SET transcripcion = 'El sol brilla. Los niños juegan en el parque.',
    puntaje_ia = 85
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3';

UPDATE evidencia
SET puntaje_ia = 80
WHERE texto = 'Mi mamá prepara sopa. Yo pongo la mesa. Luego comemos juntos.';

UPDATE evidencia
SET transcripcion = 'Había una vez un conejo que quería volar. Soplaba y soplaba, pero no despegaba.',
    puntaje_ia = 92
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/minji-nivel3.mp3';

UPDATE evidencia
SET transcripcion = 'Mi mamá me ama. Yo la abrazo todos los días.',
    puntaje_ia = 70
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/breysi-nivel1.mp3';

UPDATE evidencia
SET puntaje_ia = 65
WHERE texto = 'El perro corre en el patio. Después bebe agua.';

UPDATE evidencia
SET puntaje_ia = 30
WHERE texto = 'Me gusta jugar con mis amigos en el recreo.';

UPDATE evidencia
SET puntaje_ia = 55
WHERE texto = 'Mi casa es grande. Tiene un jardín con flores.';

UPDATE evidencia
SET transcripcion = 'El patito feo se fue solo porque los demás se burlaban de él.',
    puntaje_ia = 60
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/ester-nivel3.mp3';

UPDATE evidencia
SET transcripcion = 'La vaca come pasto. El perro corre en el campo.',
    puntaje_ia = 75
WHERE audio_url = 'https://cdn.ejemplo.pe/evidencias/ester-nivel1.mp3';
