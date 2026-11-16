-- T-319 (US-17): consigna de los niveles de escritura.
-- Es lo que el niño tiene que escribir en ese nivel, con una pista corta, el dibujo que muestra
-- la app (T-321) y cuántas palabras debe escribir como mínimo (lo usa el contador de T-324).
-- nivel_id no tiene llave foránea porque la tabla nivel es del módulo docente (V7 de Noemí),
-- igual que en V3: así el CI, que corre con la base vacía, no falla.
CREATE TABLE consigna_nivel (
    nivel_id         SMALLINT     PRIMARY KEY,                 -- un nivel de escritura tiene una consigna
    consigna         VARCHAR(300) NOT NULL,
    pista            VARCHAR(300),                             -- ayuda corta para empezar a escribir
    ilustracion      VARCHAR(30)  NOT NULL DEFAULT 'lapiz',    -- nombre del dibujo en la app
    palabras_minimas SMALLINT     NOT NULL DEFAULT 10
                     CHECK (palabras_minimas BETWEEN 1 AND 300),
    actualizado_en   TIMESTAMP    NOT NULL DEFAULT now()
);

-- Datos semilla: los dos niveles de escritura de V7 (id 2 "Palabras nuevas" e id 5 "Mi relato").
-- Los ids coinciden con el orden en que V7 insertó los niveles.
INSERT INTO consigna_nivel (nivel_id, consigna, pista, ilustracion, palabras_minimas) VALUES
(2, 'Escribe tres oraciones sobre tu animal favorito.',
    'Cuenta cómo es, qué come y dónde vive.', 'animal', 15),
(5, 'Escribe un relato corto sobre un día que nunca vas a olvidar.',
    'Piensa qué pasó primero, qué pasó después y cómo terminó.', 'libro', 40);
