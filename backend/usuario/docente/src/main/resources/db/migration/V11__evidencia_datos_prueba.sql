-- T-297 (US-31): datos de prueba de evidencias de audio y texto.
-- Se cargan 9 evidencias del aula "Noemi De La Cruz" repartidas entre sus 3
-- alumnos: 6 pendientes de revisión (3 de audio y 3 de texto), 2 aprobadas y
-- 1 rechazada. Así la lista de evidencias (T-299) tiene datos para el filtro
-- por tipo y para los tres estados.
-- Se insertan solo si el aula y los alumnos existen; en el CI la base está vacía.
INSERT INTO evidencia (estudiante_id, nivel_id, tipo, audio_url, texto,
                       resultado_ia, estado_revision, creado_en, revisado_en)
SELECT e.id,
       n.id,
       v.tipo,
       v.audio_url,
       v.texto,
       v.resultado_ia,
       v.estado_revision,
       now() - (v.dias_atras * interval '1 day'),
       -- las pendientes todavía no tienen fecha de revisión
       CASE WHEN v.estado_revision = 'pendiente' THEN NULL
            ELSE now() - ((v.dias_atras - 1) * interval '1 day')
       END
FROM estudiante e
JOIN aula a ON a.id = e.aula_id
JOIN (VALUES
    ('minji prueba', 'delacruz', 1::smallint, 'audio'::varchar,
     'https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3'::varchar, NULL::text,
     'Lee con claridad y buen ritmo. Se sugiere practicar las pausas.'::text,
     'pendiente'::varchar, 0),

    ('minji prueba', 'delacruz', 2, 'texto', NULL,
     'Mi mamá prepara sopa. Yo pongo la mesa. Luego comemos juntos.',
     'Escribe oraciones completas; revisar el uso de mayúsculas al inicio.',
     'pendiente', 2),

    ('minji prueba', 'delacruz', 3, 'audio',
     'https://cdn.ejemplo.pe/evidencias/minji-nivel3.mp3', NULL,
     'Relato ordenado y expresivo. Cumple el reto de oralidad.',
     'aprobada', 6),

    ('Breysi', 'Zavala', 1, 'audio',
     'https://cdn.ejemplo.pe/evidencias/breysi-nivel1.mp3', NULL,
     'Pronuncia con seguridad; subir un poco el volumen.',
     'pendiente', 1),

    ('Breysi', 'Zavala', 2, 'texto', NULL,
     'El perro corre en el patio. Después bebe agua.',
     'Ideas claras; separar las oraciones con punto.',
     'pendiente', 3),

    ('Breysi', 'Zavala', 5, 'texto', NULL,
     'Me gusta jugar con mis amigos en el recreo.',
     'El texto no responde al reto de la lectura asignada. Se solicita reescribir.',
     'rechazada', 7),

    ('Ester', 'Zavala', 2, 'texto', NULL,
     'Mi casa es grande. Tiene un jardín con flores.',
     'Escribe frases cortas; reforzar el uso de conectores.',
     'pendiente', 1),

    ('Ester', 'Zavala', 3, 'audio',
     'https://cdn.ejemplo.pe/evidencias/ester-nivel3.mp3', NULL,
     'Cuenta el cuento con apoyo de imágenes; practicar la entonación.',
     'pendiente', 4),

    ('Ester', 'Zavala', 1, 'audio',
     'https://cdn.ejemplo.pe/evidencias/ester-nivel1.mp3', NULL,
     'Lectura pausada y correcta. Buen avance.',
     'aprobada', 9)
) AS v (nombres, apellidos, nivel_orden, tipo, audio_url, texto,
        resultado_ia, estado_revision, dias_atras)
  ON lower(e.nombres) = lower(v.nombres)
 AND lower(e.apellidos) = lower(v.apellidos)
JOIN nivel n ON n.orden = v.nivel_orden
WHERE a.nombre = 'Noemi De La Cruz';
