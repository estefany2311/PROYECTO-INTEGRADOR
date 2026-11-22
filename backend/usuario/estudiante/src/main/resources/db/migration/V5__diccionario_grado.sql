-- T-329 (US-19): palabras que el corrector acepta en cada grado aunque no estén en el diccionario
CREATE TABLE diccionario_grado (
    id        BIGSERIAL   PRIMARY KEY,
    grado     SMALLINT    NOT NULL CHECK (grado BETWEEN 1 AND 6),
    palabra   VARCHAR(60) NOT NULL,
    creado_en TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX diccionario_grado_unica ON diccionario_grado (grado, lower(palabra));

-- Palabras peruanas y de la vida del niño que el diccionario general no trae (las de Daniela, US-52, no se repiten)
INSERT INTO diccionario_grado (grado, palabra)
SELECT g, p
FROM generate_series(1, 6) AS g
CROSS JOIN unnest(ARRAY[
    'cuy', 'cuyes', 'chompa', 'chompas', 'chaufa', 'anticucho', 'anticuchos', 'lúcuma', 'lúcumas',
    'huaico', 'huaicos', 'ceviche', 'cebiche', 'quinua', 'kiwicha', 'tambo', 'chullo', 'chullos',
    'olluco', 'ollucos', 'mashua', 'mototaxi', 'mototaxis', 'lonchera', 'loncheras', 'plastilina',
    'pingüino', 'pingüinos', 'tablet', 'Kiko'
]) AS p;
